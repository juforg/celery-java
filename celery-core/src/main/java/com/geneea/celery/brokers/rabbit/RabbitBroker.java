package com.geneea.celery.brokers.rabbit;

import com.geneea.celery.spi.Broker;
import com.geneea.celery.spi.Message;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ broker delivers messages to the workers.
 */
@Slf4j
public class RabbitBroker implements Broker {

    final Connection connection;
    private final LoadingCache<Long, Channel> channels = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .removalListener(new RemovalListener<Long, Channel>() {
                @Override
                public void onRemoval(RemovalNotification<Long, Channel> notification) {
                    if (!connection.isOpen()) {
                        return;
                    }
                    try {
                        notification.getValue().abort();
                    } catch (IOException e) {
                        log.info("Error when closing channel.", e);
                    }
                }
            }).build(new CacheLoader<Long, Channel>() {
                @Override
                public Channel load(Long key) throws Exception {
                    return connection.createChannel();
                }
            });

    public RabbitBroker(Connection connection) {
        this.connection = connection;
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
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
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
