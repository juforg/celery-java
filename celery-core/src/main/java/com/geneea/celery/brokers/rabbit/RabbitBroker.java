package com.geneea.celery.brokers.rabbit;

import com.geneea.celery.spi.Broker;
import com.geneea.celery.spi.Message;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ broker delivers messages to the workers.
 */
@Slf4j
public class RabbitBroker implements Broker {

    private final Connection connection;
    private final LoadingCache<Long, Channel> channels = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .removalListener(this::closeRemovedChannel)
            .build(CacheLoader.from(this::createChannel));

    public RabbitBroker(Connection connection) {
        this.connection = connection;
    }

    private Channel createChannel() {
        try {
            return connection.createChannel();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void closeRemovedChannel(RemovalNotification<Long, Channel> notification) {
        if (!connection.isOpen()) {
            return;
        }
        try {
            Channel channel = notification.getValue();
            if (channel != null) {
                channel.abort();
            } else {
                log.warn("RemovalNotification without channel, cause={}", notification.getCause());
            }
        } catch (IOException e) {
            log.warn("Error when closing channel.", e);
        }
    }

    @Override
    public void declareQueue(String name) throws IOException {
        getChannel().queueDeclare(name, true, false, false, null);
    }

    /**
     * @return channel usable by the current thread (may return different channels on subsequent calls)
     * @throws IOException if the channel opening fails
     */
    Channel getChannel() throws IOException {
        try {
            return channels.get(Thread.currentThread().getId());
        } catch (ExecutionException | UncheckedExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else if (cause instanceof UncheckedIOException) {
                throw ((UncheckedIOException) cause).getCause();
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        }
    }

    @Override
    public Message newMessage() {
        return new RabbitMessage(this);
    }

    @Override
    public void close() throws IOException {
        connection.abort();
    }
}
