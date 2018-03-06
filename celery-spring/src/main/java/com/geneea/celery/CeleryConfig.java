package com.geneea.celery;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.ExecutorService;

/**
 * Special configuration for the Celery-Spring library. It can be used to specify
 * various parameters and options, specially the {@link #workersEnabled} option.
 * <p>
 * It needs to exist as a {@code @Bean} or {@code @Component} in the Spring context!
 */
public interface CeleryConfig {

    String DEFAULT_QUEUE = "celery";

    /**
     * @return connection to broker that will dispatch messages
     */
    String getBrokerUri();

    /**
     * @return connection to backend providing responses,
     *         {@code null} by default
     */
    default String getBackendUri() {
        return null;
    }

    /**
     * @return routing tag (specifies into which queue the messages will go),
     *         {@code "celery"} by default
     */
    default String getQueueName() {
        return DEFAULT_QUEUE;
    }

    /**
     * @return the used executor service,
     *         {@code null} by default
     */
    default ExecutorService getExecutorService() {
        return null;
    }

    /**
     * @return the used JSON mapper,
     *         {@code null} by default
     */
    default ObjectMapper getJsonMapper() {
        return null;
    }

    /**
     * @return {@code true} iff the {@link CeleryWorker} should be started,
     *         {@code false} by default
     */
    default boolean workersEnabled() {
        return false;
    }

    /**
     * @return the number of {@link CeleryWorker} threads to be started,
     *         {@code Runtime.getRuntime().availableProcessors()} by default
     */
    default int getNumOfWorkers() {
        return Runtime.getRuntime().availableProcessors();
    }
}
