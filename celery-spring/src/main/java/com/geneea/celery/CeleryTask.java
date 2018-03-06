package com.geneea.celery;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks your code as a <em>CeleryTask</em> and a Spring {@link Component @Component}.
 * <p>
 * Each task can be executed locally by the {@link CeleryWorker} listening on RabbitMQ queue
 * or it can be executed remotely by auto-generated proxy through the {@link Celery} client.
 * <p>
 * All parameters and return types must be JSON-serializable.
 *
 * @see Celery
 * @see CeleryWorker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@TaskQualifier
public @interface CeleryTask {

}
