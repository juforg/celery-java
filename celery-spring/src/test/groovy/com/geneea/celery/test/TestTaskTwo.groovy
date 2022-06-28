package com.geneea.celery.test

import com.geneea.celery.CeleryTask

@CeleryTask
class TestTaskTwo {

    void nop() {
        // nop
    }

    double plus(double x, double y) {
        x + y
    }
}
