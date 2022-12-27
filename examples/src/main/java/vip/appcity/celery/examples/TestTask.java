package vip.appcity.celery.examples;

import vip.appcity.celery.CeleryTask;

@CeleryTask
public class TestTask {

    public int sum(int x, int y) {
        return x + y;
    }
}
