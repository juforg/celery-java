package com.geneea.examples

import com.geneea.celery.WorkerException
import com.geneea.celery.examples.ExamplesApplication
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.impl.ForgivingExceptionHandler
import org.junit.Rule
import org.rnorth.ducttape.unreliables.Unreliables
import org.testcontainers.containers.GenericContainer
import spock.genesis.Gen
import spock.lang.Specification

import java.util.concurrent.ExecutionException
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

class BasicTasksTest extends Specification {

    static final int RABBIT_PORT = 5672

    String rabbitHost(GenericContainer rabbit) {
        return "${rabbit.getContainerIpAddress()}:${rabbit.getMappedPort(RABBIT_PORT)}"
    }

    class RabbitWaitStrategy extends GenericContainer.AbstractWaitStrategy {

        @Override
        protected void waitUntilReady() {
            def f = new ConnectionFactory()
            f.uri = "amqp://guest:guest@${rabbitHost(container)}/%2F"
            f.exceptionHandler = new ForgivingExceptionHandler() {
                void log(String message, Throwable e) {} // don't log anything
            }
            Unreliables.retryUntilSuccess(startupTimeout.seconds as int, TimeUnit.SECONDS, {
                f.newConnection(ForkJoinPool.commonPool())
            })
        }
    }

    @Rule
    GenericContainer rabbit = new GenericContainer("rabbitmq:3-management")
            .withExposedPorts(RABBIT_PORT)
            .waitingFor(new RabbitWaitStrategy())

    ExamplesApplication app

    def setup() {
        System.setProperty(ExamplesApplication.RABBIT_HOST_SYS_PROP, rabbitHost(rabbit))
        app = ExamplesApplication.startSpringApp()
    }

    def "We should get the result computed by a basic task"() {
        def result
        when:
        result = app.testTaskProxy.sum(a, b)
        then:
        result.get() == app.testTasky.sum(a, b)

        where:
        a << Gen.integer(0, (Integer.MAX_VALUE / 2) as int).take(1)
        b << Gen.integer(0, (Integer.MAX_VALUE / 2) as int).take(1)
    }

    def "The future of a void task should be completed eventually"() {
        def result
        when:
        result = app.voidTaskProxy.run(-7, 12)
        then:
        result.get() == null
        result.isDone()
    }

    def "The task returning an exception should report it"() {
        WorkerException exception
        when:
        try {
            task(app.badTaskProxy).get()
        } catch (ExecutionException t) {
            exception = t.cause
        }
        then:
        exception.message == expectedMessage

        where:
        task                                      | expectedMessage
        {it -> it.throwCheckedException() }       | "Exception(null)"
        {it -> it.throwUncheckedException() }     | "RuntimeException(null)"
    }
}
