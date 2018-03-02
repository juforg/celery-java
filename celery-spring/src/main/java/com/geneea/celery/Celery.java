package com.geneea.celery;

import com.geneea.celery.spi.BackendFactory;
import com.geneea.celery.spi.BrokerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;

/**
 * A client allowing you to submit a task and get a {@link ListenableFuture} describing the result.
 * <p>
 * ....
 */
@Lazy
@Component
public class Celery extends CeleryClientCore {

    private final ImmutableList<BrokerFactory> brokers;
    private final ImmutableList<BackendFactory> backends;

    @Autowired
    public Celery(
            final CeleryConfig config,
            final List<BrokerFactory> brokers,
            final List<BackendFactory> backends
    ) {
        super(config.getBrokerUri(), config.getBackendUri(), config.getQueueName(),
              config.getExecutorService(), config.getJsonMapper());
        this.brokers = ImmutableList.copyOf(brokers);
        this.backends = ImmutableList.copyOf(backends);
    }

    @Override
    protected Iterable<BrokerFactory> findBrokers() {
        return brokers;
    }

    @Override
    protected Iterable<BackendFactory> findBackends() {
        return backends;
    }

    @PreDestroy
    private void preDestroy() throws IOException {
        close();
    }
}
