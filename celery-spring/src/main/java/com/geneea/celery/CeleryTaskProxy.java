package com.geneea.celery;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * The abstract base for auto-generated <em>CeleryTask</em> proxies which enable to
 * execute such tasks remotely via the {@link Celery} client.
 * <p>
 * This class should not be used directly, the task proxies are generated automatically
 * via annotation processing of the {@link CeleryTask @CeleryTask} annotation. For example
 * a task class {@code MyTask} will have a proxy called {@code MyTaskProxy}.
 *
 * @see com.geneea.celery.annotationprocessor.CeleryTaskProcessor
 */
public abstract class CeleryTaskProxy {

    @Autowired
    protected Celery client;

    /**
     * @return the class of the proxied task object
     */
    public abstract Class<?> getTaskClass();

    /**
     * Submits a task for processing to the client.
     */
    protected final <R> ListenableFuture<R> submit(final String method, final Object[] args) throws IOException {
        return client.submit(getTaskClass(), method, args);
    }
}
