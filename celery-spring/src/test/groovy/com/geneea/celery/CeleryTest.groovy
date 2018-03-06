package com.geneea.celery

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = [TestingConfiguration, CelerySpringConfiguration])
class CeleryTest extends Specification {

    @Autowired
    Celery client

    @Autowired
    TestingConfiguration testConfig

    def "client should find MockBrokerFactory"() {
        def foundBroker
        when:
        client.findBrokers().each {
            if (it.protocols == ["mock"] as Set)
                foundBroker = it.createBroker(null, null)
        }
        then:
        foundBroker == testConfig.broker
    }

    def "client should find MockBackendFactory"() {
        def foundBackend
        when:
        client.findBackends().each {
            if (it.protocols == ["mock"] as Set)
                foundBackend = it.createBackend(null, null)
        }
        then:
        foundBackend == testConfig.backend
    }
}
