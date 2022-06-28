package com.geneea.celery

import com.geneea.celery.test.TestTaskOne
import com.geneea.celery.test.TestTaskTwo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = [TestingConfiguration, CelerySpringConfiguration])
class CeleryTaskRegistryTest extends Specification {

    @Autowired
    CeleryTaskRegistry registry

    @Autowired
    TestTaskOne taskOne

    @Autowired
    TestTaskTwo taskTwo

    def "check all found Celery tasks"() {
        expect:
        registry.knownTasks() == [taskOne, taskTwo]
    }

    def "test finding Celery tasks by name"() {
        expect:
        registry.getTask("com.geneea.celery.test.TestTaskOne") == taskOne
        registry.getTask("com.geneea.celery.test.TestTaskTwo") == taskTwo
        registry.getTask("some.unknown.Test") == null
    }
}
