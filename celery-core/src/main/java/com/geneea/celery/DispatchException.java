package com.geneea.celery;

/**
 * An exception that occurred when trying to figure out the method that should process the task in the worker.
 */
public class DispatchException extends Exception {

    DispatchException(String msg, Object... params) {
        super(String.format(msg, params));
    }

    DispatchException(Throwable cause, String msg, Object... params) {
        super(String.format(msg, params), cause);
    }
}
