package com.geneea.celery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Internal, bootstraps a Spring context for the Celery-Spring library.
 */
@Slf4j
@Configuration
@ComponentScan
class CelerySpringConfiguration implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(final AnnotationMetadata metadata, final BeanDefinitionRegistry registry) {
        // some additional config may be here
    }
}
