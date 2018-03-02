package com.geneea.celery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Connection;
import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * A worker that listens on RabbitMQ queue and executes <em>CeleryTask</em>.
 * <p>
 * Rather than using it directly, you should just provide appropriate configuration
 * as {@link CeleryConfig} bean. Then the workers are started automatically.
 * <p>
 * The tasks which it can execute need to be annotated by {@link CeleryTask @CeleryTask}.
 */
public class CeleryWorker extends CeleryWorkerCore {

    private final CeleryTaskRegistry taskRegistry;

    @Builder
    CeleryWorker(
            @Nonnull final Connection connection,
            @Nonnull final CeleryTaskRegistry taskRegistry,
            @Nullable final String queue,
            @Nullable final ObjectMapper jsonMapper
    ) throws IOException {
        super(connection, queue, jsonMapper);
        this.taskRegistry = taskRegistry;
    }

    @Override
    protected Object findTask(final String className) {
        return taskRegistry.getTask(className);
    }
}
