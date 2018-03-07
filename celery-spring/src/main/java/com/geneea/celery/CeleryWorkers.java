package com.geneea.celery;

import com.rabbitmq.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Internal, when configured ({@link CeleryConfig#workersEnabled()}), it creates
 * and starts the required number of {@link CeleryWorker CeleryWorker} instances.
 */
@Slf4j
@Component
class CeleryWorkers {

    private final CeleryConfig config;
    private final CeleryTaskRegistry taskRegistry;

    private final List<CeleryWorker> workers = new LinkedList<>();

    @Autowired
    public CeleryWorkers(
            final CeleryConfig config,
            final CeleryTaskRegistry taskRegistry
    ) {
        this.config = config;
        this.taskRegistry = taskRegistry;
    }

    public List<CeleryWorker> getWorkers() {
        return Collections.unmodifiableList(workers);
    }

    @PostConstruct
    void startWorkers() {
        if (config.workersEnabled() && config.getNumOfWorkers() > 0) {
            final String uri = config.getBrokerUri();
            final Connection connection;
            try {
                connection = CeleryWorker.connect(uri, config.getExecutorService());
            } catch (IOException | TimeoutException | IllegalArgumentException e) {
                log.error(String.format("could not connect to the URI=\"%s\"", uri), e);
                throw new RuntimeException(e);
            }
            try {
                for (int i = 0; i < config.getNumOfWorkers(); i++) {
                    final CeleryWorker worker = CeleryWorker.builder()
                            .connection(connection)
                            .taskRegistry(taskRegistry)
                            .queue(config.getQueueName())
                            .jsonMapper(config.getJsonMapper())
                            .build();
                    workers.add(worker);
                    worker.start();
                }
            } catch (IOException e) {
                log.error(String.format("unable to start CeleryWorker for URI=\"%s\"", uri), e);
                throw new RuntimeException(e);
            }
            log.info("started {} CeleryWorkers for URI=\"{}\"", config.getNumOfWorkers(), uri);
        } else {
            log.debug("no workers are started since \"workersEnabled\"=false or \"numOfWorkers\" < 0");
        }
    }

    @PreDestroy
    private void preDestroy() throws IOException {
        for (CeleryWorker worker : workers) {
            worker.close();
        }
    }
}
