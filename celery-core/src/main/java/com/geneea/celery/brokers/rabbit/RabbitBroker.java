package com.geneea.celery.brokers.rabbit;

import com.geneea.celery.spi.Broker;
import com.geneea.celery.spi.Message;

import com.rabbitmq.client.Channel;

import java.io.IOException;

/**
 * RabbitMQ broker delivers messages to the workers.
 */
public class RabbitBroker implements Broker {

    private final Channel channel;

    public RabbitBroker(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void declareQueue(String name) throws IOException {
        channel.queueDeclare(name, true, false, false, null);
    }

    @Override
    public Message newMessage() {
        return new RabbitMessage(channel);
    }

    @Override
    public void close() throws IOException {
        channel.abort();
    }
}
