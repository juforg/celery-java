package com.geneea.celery.test;

import com.geneea.celery.CeleryTask;

import java.util.List;

@CeleryTask
public interface TestTaskInterface {

    int sum(int x, int y);

    String asStr(List<String> s);

    default void nop() {
        // nop
    }

    static void staticMethod(String abc) {}
}
