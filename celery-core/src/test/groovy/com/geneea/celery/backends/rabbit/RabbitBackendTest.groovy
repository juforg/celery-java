package com.geneea.celery.backends.rabbit

import com.geneea.celery.WorkerException
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import groovy.json.JsonSlurper
import spock.genesis.Gen
import spock.lang.Specification

import java.util.concurrent.ExecutionException

class RabbitBackendTest extends Specification {

    def Channel channel
    def RabbitBackend backend

    def setup() {
        channel = Mock(Channel.class)
        backend = new RabbitBackend(channel)
    }

    def "Backend should use the RabbitResultConsumer"() {
        def resultsProvider
        def consumerArg

        when:
        resultsProvider = backend.resultsProviderFor(clientId)

        then:
        1 * channel.queueDeclare(clientId, false, false, true, ["x-expires": 24 * 3600 * 1000])
        1 * channel.basicConsume(clientId, { consumerArg = it })
        resultsProvider == consumerArg

        where:
        clientId << Gen.string(50).take(8)
    }

    def "Backend should report successful result"() {
        def BasicProperties props
        def result

        when:
        backend.reportResult(taskId, queue, correlationId, data)
        then:
        1 * channel.basicPublish("", queue, { props = it}, { result = new JsonSlurper().parse(it, "utf-8")})
        props.correlationId == correlationId
        props.contentType == "application/json"
        props.contentEncoding == "utf-8"
        props.deliveryMode == 1  // non-peristent
        result["status"] == "SUCCESS"
        result["task_id"] == taskId
        result["result"] == data

        where:
        correlationId << Gen.string(50).take(3)
        taskId << Gen.string(50).take(3)
        queue << Gen.string(20).take(3)
        data << [["x"], ["a": 1, "b": ["x"]], 12]
    }

    def "Backend should report exception"() {
        def BasicProperties props
        def result

        when:
        backend.reportException(taskId, queue, correlationId, data)
        then:
        1 * channel.basicPublish("", queue, { props = it}, { result = new JsonSlurper().parse(it, "utf-8")})
        props.correlationId == correlationId
        props.contentType == "application/json"
        props.contentEncoding == "utf-8"
        props.deliveryMode == 1  // non-peristent
        result["status"] == "FAILURE"
        result["task_id"] == taskId
        result["result"]["exc_type"] == data.class.simpleName
        result["result"]["exc_message"] == data.message

        where:
        correlationId << Gen.string(50).take(3)
        taskId << Gen.string(50).take(3)
        queue << Gen.string(20).take(3)
        data << [new IOException("Xdan"), new AssertionError("Bada"), new RuntimeException()]
    }
}

class RabbitResultConsumerTest extends Specification {

    def Channel channel
    def RabbitBackend backend
    def RabbitResultConsumer consumer

    def setup() {
        channel = Mock(Channel.class)
        backend = new RabbitBackend(channel)
        consumer = new RabbitResultConsumer(backend)
    }

    def "Consumer should report result of a task"() {
        def result = consumer.getResult(taskId)

        when:
        consumer.handleDelivery(null, new Envelope(1, false, "", ""), null, body.bytes)

        then:
        result.isDone() == done
        !result.isDone() || result.get() == expectedResult

        where:
        body                                                                                        | expectedResult | taskId      | done
        '{"children":[], "status": "SUCCESS", "result": 1, "traceback": null, "task_id": "xac"}'    | 1              | "xac"       | true
        '{"children":[], "status": "SUCCESS", "result": "x", "traceback": null, "task_id": "1aa"}'  | "x"            | "1aa"       | true
        // These results are for different tasks - this task should not be affected
        '{"children":[], "status": "SUCCESS", "result": "x", "traceback": null, "task_id": "1ac"}'  | null           | "1aa"       | false
        '{"children":[], "status": "SUCCESS", "result": 1, "traceback": null, "task_id": "1ac"}'    | null           | "1aa"       | false
    }

    def "Consumer should report a received error"() {
        def result = consumer.getResult("1aa")
        def ex

        when:
        consumer.handleDelivery(null, new Envelope(1, false, "", ""), null, body.bytes)

        then:
        result.isDone()

        when:
        try {
            result.get()
        } catch(ExecutionException e) {
            ex = e.cause
        }

        then:
        ex instanceof WorkerException
        ex.message.length() > 0

        where:
        body << [
                '{"children":[], ' +
                        '"status": "FAILURE", ' +
                        '"result": {"exc_type": "ExType", "exc_message": "Bad, bad error"}, ' +
                        '"traceback": null, "task_id": "1aa"}',
                '{' +
                        '"children":[], ' +
                        '"status": "FAILURE", ' +
                        '"result": {"exc_type": "AnotherType", "exc_message": "Not so useful"}, ' +
                        '"traceback": null, ' +
                        '"task_id": "1aa"}',
        ]
    }
}
