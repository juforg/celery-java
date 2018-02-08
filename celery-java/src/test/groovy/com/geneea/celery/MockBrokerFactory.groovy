package com.geneea.celery

import com.geneea.celery.spi.Broker
import com.geneea.celery.spi.BrokerFactory

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeoutException

public class MockBrokerFactory implements BrokerFactory {
    static broker

    @Override
    Set<String> getProtocols() {
        return ["mock"]
    }

    @Override
    Broker createBroker(URI uri, ExecutorService executor) throws IOException, TimeoutException {
        return broker
    }
}
