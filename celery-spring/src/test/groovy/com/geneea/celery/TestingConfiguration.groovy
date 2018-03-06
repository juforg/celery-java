package com.geneea.celery

import com.geneea.celery.spi.Backend
import com.geneea.celery.spi.BackendFactory
import com.geneea.celery.spi.Broker
import com.geneea.celery.spi.BrokerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import spock.mock.DetachedMockFactory

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeoutException

@Configuration
@ComponentScan("com.geneea.celery.test")
class TestingConfiguration {

    DetachedMockFactory factory = new DetachedMockFactory()
    Backend backend = factory.Mock(Backend)
    Broker broker = factory.Mock(Broker)

    @Component
    class TestingCeleryConfig implements CeleryConfig {

        String getBrokerUri() {
            "mock://anything"
        }

        String getBackendUri() {
            "mock://anything"
        }
    }

    @Bean
    MockBackendFactory mockBackendFactory() {
        new MockBackendFactory()
    }

    @Bean
    MockBrokerFactory mockBrokerFactory() {
        new MockBrokerFactory()
    }

    class MockBackendFactory implements BackendFactory {
        @Override
        Set<String> getProtocols() {
            return ["mock"]
        }

        @Override
        Backend createBackend(URI uri, ExecutorService executor) throws IOException, TimeoutException {
            return backend
        }
    }

    class MockBrokerFactory implements BrokerFactory {
        @Override
        Set<String> getProtocols() {
            return ["mock"]
        }

        @Override
        Broker createBroker(URI uri, ExecutorService executor) throws IOException, TimeoutException {
            return broker
        }
    }
}
