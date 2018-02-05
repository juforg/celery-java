
package com.geneea.celery;

import com.geneea.celery.backends.rabbit.RabbitBackend;
import com.geneea.celery.spi.Backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Streams;
import com.google.common.primitives.Primitives;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * CeleryWorker that listens on RabbitMQ queue and executes tasks. You can either embed it into your project via
 * {@link #create(String, Connection)} or start it stand-alone via {@link CeleryWorkerCLI}.
 */
@Slf4j
public class CeleryWorker extends DefaultConsumer {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Lock taskRunning = new ReentrantLock();
    private final Backend backend;

    CeleryWorker(Channel channel, Backend backend) {
        super(channel);
        this.backend = backend;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        String taskId = properties.getHeaders().get("id").toString();
        taskRunning.lock();
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            String message = new String(body, properties.getContentEncoding());

            JsonNode payload = jsonMapper.readTree(message);

            String taskClassName = properties.getHeaders().get("task").toString();
            Object result = processTask(
                    taskClassName,
                    (ArrayNode) payload.get(0),
                    (ObjectNode) payload.get(1));

            log.info("CeleryTask {}[{}] succeeded in {}", taskClassName, taskId, stopwatch);
            log.debug("CeleryTask {}[{}] result was: {}", taskClassName, taskId, result);

            backend.reportResult(taskId, properties.getReplyTo(), properties.getCorrelationId(), result);

            getChannel().basicAck(envelope.getDeliveryTag(), false);
        } catch (DispatchException e) {
            log.error(String.format("CeleryTask %s dispatch error", taskId), e.getCause());
            backend.reportException(taskId, properties.getReplyTo(), properties.getCorrelationId(), e);
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        } catch (InvocationTargetException e) {
            log.error(String.format("CeleryTask %s error", taskId), e.getCause());
            backend.reportException(taskId, properties.getReplyTo(), properties.getCorrelationId(), e.getCause());
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        } catch (JsonProcessingException e) {
            log.error(String.format("CeleryTask %s - processing error", taskId), e);
            backend.reportException(taskId, properties.getReplyTo(), properties.getCorrelationId(), e);
            getChannel().basicNack(envelope.getDeliveryTag(), false, false);
        } catch (RuntimeException e) {
            log.error(String.format("CeleryTask %s - runtime error", taskId), e);
            backend.reportException(taskId, properties.getReplyTo(), properties.getCorrelationId(), e.getCause() != null ? e.getCause() : e);
            getChannel().basicNack(envelope.getDeliveryTag(), false, false);
        } finally {
            taskRunning.unlock();
        }
    }

    private Object processTask(String taskName, ArrayNode args, ObjectNode kwargs)
            throws DispatchException, InvocationTargetException {

        List<String> name = ImmutableList.copyOf(Splitter.on("#").split(taskName).iterator());

        if (name.size() != 2) {
            throw new DispatchException(MessageFormat.format(
                    "This worker can only process tasks with name in form package.ClassName#method, got {}",
                    taskName));
        }

        Object task = TaskRegistry.getTask(name.get(0));

        if (task == null) {
            throw new DispatchException(String.format("CeleryTask %s not registered.", taskName));
        }

        Method method = Arrays.stream(task.getClass().getDeclaredMethods())
                .filter((m) -> m.getName().equals(name.get(1)))
                .collect(MoreCollectors.onlyElement());

        List<?> convertedArgs = Streams.mapWithIndex(
                Arrays.stream(method.getParameterTypes()),
                              (paramType, i) -> jsonMapper.convertValue(args.get((int) i), paramType)
        ).collect(Collectors.toList());

        try {
            return method.invoke(task, convertedArgs.toArray());
        } catch (IllegalAccessException e) {
            throw new DispatchException(String.format("Error calling %s", method), e);
        } catch (IllegalArgumentException e) {
            // should not happen because of findRunMethod
            throw new AssertionError(String.format("Error calling %s", method), e);
        }
    }

    public void close() throws IOException {
        getChannel().abort();
        backend.close();
    }

    public void join() {
        taskRunning.lock();
        taskRunning.unlock();
    }

    private static Optional<java.lang.reflect.Method> findRunMethod(Class<?> cls, List<Class<?>> args) {
        return Arrays.stream(cls.getDeclaredMethods())
                .filter((m) -> m.getName().equals("run"))
                .filter((m) -> {
                    Class<?>[] parameterTypes = m.getParameterTypes();
                    if (parameterTypes.length != args.size()) {
                        return false;
                    }

                    for (int i = 0; i < args.size(); i++) {
                        if (!Primitives.wrap(parameterTypes[i]).isAssignableFrom(args.get(i))) {
                            return false;
                        }
                    }
                    return true;
                }).findAny();
    }

    public static CeleryWorker create(String queue, Connection connection) throws IOException {
        final Channel channel = connection.createChannel();
        channel.basicQos(2);
        channel.queueDeclare(queue, true, false, false, null);
        RabbitBackend backend = new RabbitBackend(channel);
        final CeleryWorker consumer = new CeleryWorker(channel, backend);
        channel.basicConsume(queue, false, "", true, false, null, consumer);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                consumer.close();
                consumer.join();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        return consumer;
    }
}
