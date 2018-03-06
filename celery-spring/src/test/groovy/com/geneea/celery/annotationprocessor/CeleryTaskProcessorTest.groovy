package com.geneea.celery.annotationprocessor

import com.google.testing.compile.JavaFileObjects
import spock.lang.Specification

import static com.google.testing.compile.JavaSourcesSubject.assertThat

class CeleryTaskProcessorTest extends Specification {

    def "test @CeleryTask proxy generating"() {
        expect:
        assertThat(JavaFileObjects.forResource("test/TestTask.java"))
            .processedWith(new CeleryTaskProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(JavaFileObjects.forResource("expected/TestTaskProxy.java"))
    }

    def "test @CeleryTask proxy generating for interface"() {
        expect:
        assertThat(JavaFileObjects.forResource("test/TestTaskInterface.java"))
            .processedWith(new CeleryTaskProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(JavaFileObjects.forResource("expected/TestTaskInterfaceProxy.java"))
    }

}
