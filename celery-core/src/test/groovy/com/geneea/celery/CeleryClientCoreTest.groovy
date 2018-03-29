package com.geneea.celery

import com.geneea.celery.spi.Backend
import com.geneea.celery.spi.Broker
import com.geneea.celery.spi.Message
import com.google.common.util.concurrent.SettableFuture
import groovy.json.JsonSlurper
import spock.genesis.Gen
import spock.lang.Specification

class ClientTest extends Specification {

    def TestCeleryClient client

    def Message message
    def Message.Headers headers

    def setup() {
        MockBrokerFactory.queuesDeclared = []

        message = Mock(Message.class)
        headers = Mock(Message.Headers.class)

        message.getHeaders() >> headers
        MockBrokerFactory.messages = [message]

        client = new TestCeleryClient("mock://anything", null, null)
    }

    def "Client should send UTF-8 encoded JSON payload by default"() {
        when:
        client.submit(TestingTask.class, "doWork", [0.5, new Payload(prop1: "p1val")] as Object[])

        then:
        1 * message.setContentType("application/json")
        1 * message.setContentEncoding("utf-8")
        1 * message.setBody({
            new JsonSlurper().parse(it, "utf-8")[0] == [0.5, [prop1:"p1val"]] }
        )

        then:
        1 * message.send("celery")
    }

    def "Client should set task properties"() {
        def payload = new Payload(prop1: "p1val")
        def tooLongString = ""
        (1..100).each { tooLongString += Integer.toHexString(it) }

        when:
        client.submit(TestingTask.class, "doWork", [0.5, "xyz", true, 3, payload, tooLongString] as Object[])

        then:
        1 * message.headers.setArgsRepr("(0.5, \"xyz\", true, 3" \
            + ", com.geneea.celery.Payload@"+ Integer.toHexString(System.identityHashCode(payload)) \
            + ", \"" + tooLongString[0..<30] + "..." + tooLongString[-30..-1] + "\")")
        1 * message.headers.setOrigin({it.endsWith("@" + InetAddress.getLocalHost().getHostName())})
    }

    def "Task ID should be different for each task submitted"() {
        def taskIds = []
        when:
        (1..10).each {
            client.submit(TestingTask.class, "doWork", [0.5, new Payload(prop1: "p1val")] as Object[])
        }
        then:
        10 * message.headers.setId({ taskIds << it })

        (taskIds as Set).size() == 10
    }

    def "Client should send message to the right queue"() {
        client = new TestCeleryClient("mock://broker", null, queue)
        when:
        client.submit(TestingTask.class, "doWork", [0.5, new Payload(prop1: "p1val")] as Object[])
        then:
        1 * message.send(queue)
        where:
        queue << Gen.these("celery").then(Gen.string(80)).take(5)
    }

    def "Client shouldn't set reply-to if it doesn't have backend"() {
        when:
        client.submit(TestingTask.class, "doWork", [0.5, new Payload(prop1: "p1val")] as Object[])
        then:
        0 * message.headers.setReplyTo(_)
    }

    def "Client without backend should return empty completed future"() {
        def result
        when:
        result = client.submit(TestingTask.class, "doWork", [0.5, new Payload(prop1: "p1val")] as Object[])
        then:
        result.isDone()
        result.get() == null
    }

    def "Client should set task name for class"() {
        when:
        client.submit(TestingTask.class, "doWork", [] as Object[])
        then:
        1 * message.headers.setTaskName("com.geneea.celery.TestingTask#doWork")
    }

    def "Client should set task name"() {
        when:
        client.submit(taskName, [] as Object[])
        then:
        1 * message.headers.setTaskName(taskName)

        where:
        taskName << Gen.string.take(5)
    }
}

class ClientWithBackendTest extends Specification {

    def TestCeleryClient client
    def Broker broker

    def Message message
    def Message.Headers headers

    def Backend backend
    def Backend.ResultsProvider resultsProvider

    def setup() {
        MockBrokerFactory.queuesDeclared = []

        message = Mock(Message.class)
        headers = Mock(Message.Headers.class)

        message.getHeaders() >> headers
        MockBrokerFactory.messages = [message]

        backend = Mock(Backend.class)
        resultsProvider = Mock(Backend.ResultsProvider.class)
        backend.resultsProviderFor(_) >> resultsProvider
        MockBackendFactory.backend = backend

        client = new TestCeleryClient("mock://x", "mock://something", null)
    }

    def "Client ID and task ID should be different for each client"() {
        def clientIds = [], taskIds = []
        when:
        (1..10).each {
            client = new TestCeleryClient("mock://x", "mock://something", null)
            client.submit(TestingTask.class, "doWork", [0.5, new Payload(prop1: "p1val")] as Object[])
        }
        then:
        10 * message.headers.setReplyTo({ clientIds << it })
        10 * message.headers.setId({ taskIds << it })

        (clientIds as Set).size() == 10
        (taskIds as Set).size() == 10
    }

    def "Client ID should stay the same across multiple invocations"() {
        def clientIds = []

        when:
        (1..10).each {
            client.submit(TestingTask.class, "doWork", [0.5, new Payload(prop1: "p1val")] as Object[])
        }
        then:
        10 * message.headers.setReplyTo({ clientIds << it })

        (clientIds as Set).size() == 1
    }

    def "Client should return result from backend"() {
        def result = SettableFuture.create()
        resultsProvider.getResult(_) >> result

        def returnedResult

        when:
        returnedResult = client.submit(TestingTask.class, "doWork", [0.5, new Payload(prop1: "p1val")] as Object[])

        then:
        !returnedResult.isDone()

        when:
        result.set(resultVal)

        then:
        returnedResult.isDone()
        returnedResult.get() == resultVal

        where:
        resultVal << Gen.string.take(1)
    }

    def "Client should ask the backend for correct task ID"() {
        def taskId
        when:
        client.submit(TestingTask.class, "doWork", [0.5, new Payload(prop1: "p1val")] as Object[])

        then:
        1 * message.headers.setId({ taskId = it })

        and:
        taskId != null
        1 * resultsProvider.getResult({ it == taskId })
    }

    def "Client should declare queue before sending its message"() {
        when:
        client = new TestCeleryClient("mock://x", null, queue)
        client.submit(TestingTask.class, "doWork", [0.5, new Payload(prop1: "p1val")] as Object[])

        then:
        MockBrokerFactory.queuesDeclared == [queue]

        then:
        1* message.send(queue)

        where:
        queue << Gen.string.take(5)
    }
}

class MultiMessageTest extends Specification {
    def Broker broker
    def TestCeleryClient client

    def messages = []

    def setup() {
        broker = Mock(Broker)
        client = new TestCeleryClient("mock://xyz", null, null)

        (0..5).each {
            def message = Mock(Message.class)
            def headers = Mock(Message.Headers.class)
            message.getHeaders() >> headers

            messages << [message:message, headers:headers]
        }
        MockBrokerFactory.messages = messages.collect {it["message"]}
    }

    def "Client should set some random id "() {
        when:
        messages.forEach {
            client.submit(TestingTask.class, "doWork", [] as Object[])
        }

        then:
        messages.forEach {
            1 * it["headers"].setId(_)
        }
    }
}

class TestingTask {
    def doWork(a, b) {
        throw new UnsupportedOperationException()
    }
}

class Payload {
    String prop1
}
