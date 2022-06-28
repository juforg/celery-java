package com.geneea.celery.examples;

import com.geneea.celery.CeleryConfig;
import com.geneea.celery.EnableCelery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * An example application demonstrating usage of the Celery-Spring library.
 */
@EnableCelery
@SpringBootApplication
public class ExamplesApplication {

    public static final String RABBIT_HOST_SYS_PROP = "rabbitmq.host";
    public static final String RABBIT_HOST_DEFAULT = "localhost";

    @Autowired
    public TestTask testTasky;
    @Autowired
    public TestTaskProxy testTaskProxy;

    @Autowired
    public VoidTask voidTask;
    @Autowired
    public VoidTaskProxy voidTaskProxy;

    @Autowired
    public BadTask badTask;
    @Autowired
    public BadTaskProxy badTaskProxy;

    public static String getRabbitHost() {
        return System.getProperty(RABBIT_HOST_SYS_PROP, RABBIT_HOST_DEFAULT);
    }

    public static void main(final String[] args) {
        startSpringApp(args);
    }

    public static ExamplesApplication startSpringApp(final String... args) {
        final ApplicationContext context = SpringApplication.run(ExamplesApplication.class, args);
        return context.getBean(ExamplesApplication.class);
    }

    @Component
    public static class ExamplesCeleryConfig implements CeleryConfig {

        @Override
        public String getBrokerUri() {
            return "amqp://" + getRabbitHost() + "/%2F";
        }

        @Override
        public String getBackendUri() {
            return "rpc://" + getRabbitHost() + "/%2F";
        }

        @Override
        public boolean workersEnabled() {
            return true;
        }

        @Override
        public int getNumOfWorkers() {
            return 1;
        }
    }
}
