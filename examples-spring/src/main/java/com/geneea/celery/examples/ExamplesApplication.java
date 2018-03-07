package com.geneea.celery.examples;

import com.geneea.celery.CeleryConfig;
import com.geneea.celery.EnableCelery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@EnableCelery
@SpringBootApplication
public class ExamplesApplication {

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

    public static void main(final String[] args) {
        startSpringApp(args);
    }

    public static ExamplesApplication startSpringApp(final String... args) {
        final ApplicationContext context = SpringApplication.run(ExamplesApplication.class, args);
        return context.getBean(ExamplesApplication.class);
    }

    @Component
    public class ExamplesCeleryConfig implements CeleryConfig {

        @Override
        public String getBrokerUri() {
            return "amqp://localhost/%2F";
        }

        @Override
        public String getBackendUri() {
            return "rpc://localhost/%2F";
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
