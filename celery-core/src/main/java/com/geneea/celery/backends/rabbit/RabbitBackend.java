package com.geneea.celery.backends.rabbit;

import com.geneea.celery.backends.TaskResult;
import com.geneea.celery.spi.Backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 *     Backend, in Celery terminology, is a way to deliver task results back to the client.
 * </p>
 * <p>
 *     This one sends the tasks to RabbitMQ routing key specified by the reply-to property. The client should register
 *     a temporary queue with its UUID so the overhead of creating a queue happens once per client.
 * </p>
 */
public class RabbitBackend implements Backend {

    private static final String CONTENT_TYPE = "application/json";
    private static final String ENCODING = "utf-8";
    private static final ImmutableMap<String, Object> QUEUE_ARGS = ImmutableMap.of(
            "x-expires", 24 * 3600 * 1000
    );

    final Channel channel;
    final ObjectMapper jsonMapper;
    final ExecutorService executor;

    public RabbitBackend(Channel channel, ObjectMapper jsonMapper, ExecutorService executor) {
        this.channel = channel;
        this.jsonMapper = jsonMapper != null ? jsonMapper : new ObjectMapper();
        this.executor = executor != null ? executor : Executors.newCachedThreadPool();
    }

    public RabbitBackend(Channel channel, ObjectMapper jsonMapper) {
        this(channel, jsonMapper, null);
    }

    public RabbitBackend(Channel channel, ExecutorService executor) {
        this(channel, null, executor);
    }

    public RabbitBackend(Channel channel) {
        this(channel, null, null);
    }

    @Override
    public ResultsProvider resultsProviderFor(String clientId) throws IOException {
        // max number of unacknowledged messages "in-flight" from the queue to the consumer
        channel.basicQos(2, false);
        channel.queueDeclare(clientId, false, false, true, QUEUE_ARGS);
        RabbitResultConsumer consumer = new RabbitResultConsumer(this);
        channel.basicConsume(clientId, consumer);
        return consumer;
    }

    @Override
    public <R> void reportResult(String taskId, String queue, String correlationId, R result) throws IOException {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .priority(0)
                .deliveryMode(1)
                .contentType(CONTENT_TYPE)
                .contentEncoding(ENCODING)
                .build();

        TaskResult res = new TaskResult();
        res.result = jsonMapper.valueToTree(result);
        res.taskId = taskId;
        res.status = TaskResult.Status.SUCCESS;

        channel.basicPublish("", queue, properties, jsonMapper.writeValueAsBytes(res));
    }

    @Override
    public void reportException(String taskId, String replyTo, String correlationId, Throwable e) throws IOException {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .priority(0)
                .deliveryMode(1)
                .contentType(CONTENT_TYPE)
                .contentEncoding(ENCODING)
                .build();

        TaskResult res = new TaskResult();
        res.result = jsonMapper.createObjectNode()
                .put("exc_type", e.getClass().getSimpleName())
                .put("exc_message", e.getMessage());
        res.taskId = taskId;
        res.status = TaskResult.Status.FAILURE;

        channel.basicPublish("", replyTo, properties, jsonMapper.writeValueAsBytes(res));
    }

    @Override
    public void close() throws IOException {
        channel.abort();
    }
}
