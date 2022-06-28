package com.geneea.celery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Internal, bootstraps a Spring context for the Celery-Spring library.
 */
@Slf4j
@Configuration
@ComponentScan
class CelerySpringConfiguration {

    public CelerySpringConfiguration() {
        log.info("bootstrapping the Celery-Spring context");
    }
}
