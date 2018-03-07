package com.geneea.celery.backends.rabbit;

import com.geneea.celery.WorkerException;
import com.geneea.celery.backends.TaskResult;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of RabbitMQ consumer as a result provider for {@link RabbitBackend}.
 */
@Slf4j
class RabbitResultConsumer<R> extends DefaultConsumer implements RabbitBackend.ResultsProvider<R> {

    private final RabbitBackend backend;
    private final LoadingCache<String, SettableFuture<R>> tasks = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build(CacheLoader.from(SettableFuture::create));

    RabbitResultConsumer(RabbitBackend backend) {
        super(backend.channel);
        this.backend = backend;
    }

    @Override
    public ListenableFuture<R> getResult(String taskId) {
        return tasks.getUnchecked(taskId);
    }

    @Override
    public void handleDelivery(
            String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body
    ) throws IOException {
        SettableFuture<R> future = null;
        try {
            TaskResult payload = backend.jsonMapper.readValue(body, TaskResult.class);

            future = tasks.getUnchecked(payload.taskId);
            boolean setAccepted;
            if (payload.status == TaskResult.Status.SUCCESS) {
                setAccepted = future.set(payload.getResult());
            } else {
                Map<String, String> exc = payload.getResult();
                setAccepted = future.setException(new WorkerException(exc.get("exc_type"), exc.get("exc_message")));
            }
            checkState(setAccepted, "setting future was not accepted: %s", future);
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        } catch (IOException | RuntimeException e) {
            if (future != null) {
                future.setException(e);
            }
            log.error(String.format("ResultsConsumer error - %s", envelope.getDeliveryTag()), e);
            getChannel().basicNack(envelope.getDeliveryTag(), false, false);
        }
    }

    @Override
    public RabbitBackend getBackend() {
        return backend;
    }
}
