package com.geneea.celery

import com.geneea.celery.spi.*

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeoutException

class MockCeleryClient extends CeleryClientCore {

    MockCeleryClient(String brokerUri, String backendUri, String queue) {
        super(brokerUri, backendUri, queue, null, null)
    }

    @Override
    Iterable<BrokerFactory> findBrokers() {
        return [new MockBrokerFactory()]
    }

    @Override
    Iterable<BackendFactory> findBackends() {
        return [new MockBackendFactory()]
    }
}

class MockBrokerFactory implements BrokerFactory {
    static List<String> queuesDeclared = []

    /**
     * Workaround for the fact that Spock mocks can be created only from the Specification class.
     */
    static List<Message> messages
    static messageNum = 0

    @Override
    Set<String> getProtocols() {
        return ["mock"]
    }

    @Override
    Broker createBroker(URI uri, ExecutorService executor) throws IOException, TimeoutException {
        return new Broker() {
            @Override
            void declareQueue(String name) throws IOException {
                queuesDeclared.add(name)
            }

            @Override
            Message newMessage() {
                def message = messages[messageNum % messages.size()]
                messageNum++
                return message
            }

            @Override
            void close() throws IOException {
            }
        }
    }
}

class MockBackendFactory implements BackendFactory {
    static backend

    @Override
    Set<String> getProtocols() {
        return ["mock"]
    }

    @Override
    Backend createBackend(URI uri, ExecutorService executor) throws IOException, TimeoutException {
        return backend
    }
}
