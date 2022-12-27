package vip.appcity.celery.examples;

import vip.appcity.celery.CeleryTask;

@CeleryTask
public class TestVoidTask {

    public void run(int x, int y) {
        System.out.println("I'm the task that just prints: " + (x + y));
    }
}
