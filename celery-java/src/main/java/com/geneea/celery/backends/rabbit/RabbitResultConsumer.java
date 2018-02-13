package com.geneea.celery.backends.rabbit;

import com.geneea.celery.WorkerException;
import com.geneea.celery.backends.TaskResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class RabbitResultConsumer<R> extends DefaultConsumer implements RabbitBackend.ResultsProvider<R> {

    private final LoadingCache<String, SettableFuture<R>> tasks =
            CacheBuilder
                    .newBuilder()
                    .expireAfterWrite(2, TimeUnit.HOURS)
                    .build(CacheLoader.from(SettableFuture::create));
    private final ObjectMapper jsonMapper = new ObjectMapper();

    RabbitResultConsumer(Channel channel) {
        super(channel);
    }

    @Override
    public ListenableFuture<R> getResult(String taskId) {
        return tasks.getUnchecked(taskId);
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        TaskResult payload = jsonMapper.readValue(body, TaskResult.class);

        SettableFuture<R> future = tasks.getUnchecked(payload.taskId);
        boolean setAccepted;
        if (payload.status == TaskResult.Status.SUCCESS) {
            setAccepted = future.set(payload.getResult());
        } else {
            Map<String, String> exc = payload.getResult();
            setAccepted = future.setException(
                    new WorkerException(exc.get("exc_type"), exc.get("exc_message")));
        }
        assert setAccepted;
    }
}
