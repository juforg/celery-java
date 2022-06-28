package com.geneea.celery.test;

import com.geneea.celery.CeleryTask;

import java.util.List;

@CeleryTask
public class TestTask {

    public int sum(int x, int y) {
        return x + y;
    }

    public String asStr(List<String> s) {
        return s.toString();
    }

    public void nop() {
        // nop
    }

    protected void protectedMethod(String abc) {}
    private void privateMethod(String abc) {}
    public static void staticMethod(String abc) {}
}
