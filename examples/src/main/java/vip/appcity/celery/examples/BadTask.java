package vip.appcity.celery.examples;

import vip.appcity.celery.CeleryTask;

/**
 * Task that always throws exception.
 */
@CeleryTask
public class BadTask {

    public void throwCheckedException() throws Exception {
        throw new Exception();
    }

    public void throwUncheckedException() {
        throw new RuntimeException();
    }
}
