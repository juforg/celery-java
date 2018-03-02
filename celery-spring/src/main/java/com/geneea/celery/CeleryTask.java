package com.geneea.celery;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks your code as a <em>CeleryTask</em>.
 * <p>
 * ....
 * <p>
 * All parameters and return types must be JSON-serializable.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@TaskQualifier
public @interface CeleryTask {

}
