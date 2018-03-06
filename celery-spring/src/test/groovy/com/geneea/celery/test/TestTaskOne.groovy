package com.geneea.celery.test

import com.geneea.celery.CeleryTask

@CeleryTask
class TestTaskOne {

    String run(int num, String str) {
        "done"
    }
}
