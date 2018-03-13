package com.geneea.celery.test;

import com.geneea.celery.CeleryTask;

import java.util.List;

public class TestTaskFailure {

    @CeleryTask
    public static class Nested {

    }
}
