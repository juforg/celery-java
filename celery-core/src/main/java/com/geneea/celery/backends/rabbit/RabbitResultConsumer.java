package com.geneea.celery.backends.rabbit;

import com.geneea.celery.WorkerException;
import com.geneea.celery.backends.TaskResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of RabbitMQ consumer as a result provider for {@link RabbitBackend}.
 */
@Slf4j
class RabbitResultConsumer extends DefaultConsumer implements RabbitBackend.ResultsProvider {

    private final RabbitBackend backend;
    private final LoadingCache<String, SettableFuture<JsonNode>> tasks = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build(CacheLoader.from(SettableFuture::create));

    RabbitResultConsumer(RabbitBackend backend) {
        super(backend.channel);
        this.backend = backend;
    }

    @Override
    public <R> ListenableFuture<R> getResult(String taskId, Class<R> resultClass) {
        return Futures.transform(
                tasks.getUnchecked(taskId),
                node -> backend.jsonMapper.convertValue(node, resultClass),
                backend.executor
        );
    }

    @Override
    public void handleDelivery(
            String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body
    ) throws IOException {
        TaskResult payload;
        try {
            payload = backend.jsonMapper.readValue(body, TaskResult.class);
        } catch (IOException e) {
            log.error(String.format("could not read payload for deliveryTag=%d", envelope.getDeliveryTag()), e);
            getChannel().basicNack(envelope.getDeliveryTag(), false, false);
            return;
        }

        SettableFuture<JsonNode> future = tasks.getUnchecked(payload.taskId);
        boolean setAccepted;
        if (payload.status == TaskResult.Status.SUCCESS) {
            setAccepted = future.set(payload.result);
        } else {
            String excType = getStrValue(payload.result, "exc_type");
            String excMessage = getStrValue(payload.result, "exc_message");
            setAccepted = future.setException(new WorkerException(excType, excMessage));
        }

        if (setAccepted) {
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        } else {
            log.error("setting future was not accepted for deliveryTag={}", envelope.getDeliveryTag());
            getChannel().basicNack(envelope.getDeliveryTag(), false, false);
        }
    }

    @Override
    public RabbitBackend getBackend() {
        return backend;
    }

    private static String getStrValue(JsonNode node, String field) {
        return (node != null && node.hasNonNull(field)) ? node.get(field).textValue() : null;
    }
}
