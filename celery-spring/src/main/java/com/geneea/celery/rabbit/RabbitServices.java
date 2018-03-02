package com.geneea.celery.rabbit;

import com.geneea.celery.backends.rabbit.RabbitBackendFactory;
import com.geneea.celery.brokers.rabbit.RabbitBrokerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Will register services for RabbitMQ.
 */
@Lazy
@Configuration
public class RabbitServices {

    @Bean
    public RabbitBackendFactory backendFactory() {
        return new RabbitBackendFactory();
    }

    @Bean
    public RabbitBrokerFactory brokerFactory() {
        return new RabbitBrokerFactory();
    }

}
