package com.geneea.celery

import com.geneea.celery.spi.Backend
import com.geneea.celery.spi.Broker
import spock.lang.Specification

class CeleryTest extends Specification {

    def Celery client

    def setup() {
        MockBrokerFactory.broker = Mock(Broker.class)
        MockBackendFactory.backend = Mock(Backend.class)

        client = Celery.builder()
                .brokerUri("mock://anything")
                .backendUri("mock://something")
                .build()
    }

    def "Client should find MockBrokerFactory"() {
        def foundBroker
        when:
        client.findBrokers().each {
            if (it.protocols == ["mock"] as Set)
                foundBroker = it.createBroker(null, null)
        }
        then:
        foundBroker == MockBrokerFactory.broker
    }

    def "Client should find MockBackendFactory"() {
        def foundBackend
        when:
        client.findBackends().each {
            if (it.protocols == ["mock"] as Set)
                foundBackend = it.createBackend(null, null)
        }
        then:
        foundBackend == MockBackendFactory.backend
    }
}


