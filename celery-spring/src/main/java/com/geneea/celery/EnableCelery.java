package com.geneea.celery;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables support for <em>CeleryTask</em> submissions and executions.
 * To be used on {@code @Configuration} classes. It'll bootstrap the
 * Celery-Spring library for the existing Spring context.
 * <p>
 * A bean for {@link CeleryConfig} needs to exist in the Spring context
 * so that Celery-Spring library is properly configured.
 *
 * @see Celery
 * @see CeleryWorker
 * @see CeleryTask
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(CelerySpringConfiguration.class)
public @interface EnableCelery {

}
