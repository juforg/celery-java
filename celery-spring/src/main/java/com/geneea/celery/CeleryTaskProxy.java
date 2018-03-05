package com.geneea.celery;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public abstract class CeleryTaskProxy {

    @Autowired
    protected Celery client;

    public abstract Class<?> getTaskClass();

    protected final <R> ListenableFuture<R> submit(final String method, final Object[] args) throws IOException {
        return client.submit(getTaskClass(), method, args);
    }
}
