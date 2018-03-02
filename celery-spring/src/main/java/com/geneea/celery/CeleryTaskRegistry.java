package com.geneea.celery;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

/**
 * Finds all available tasks that are annotated by {@link CeleryTask @CeleryTask}.
 */
@Slf4j
@Lazy
@Component
public class CeleryTaskRegistry {

    private final ImmutableMap<String, ?> tasks;

    @Autowired
    public CeleryTaskRegistry(
            @TaskQualifier final List<?> taskObjects
    ) {
        tasks = taskObjects.stream()
                .filter(t -> validateTaskClass(t.getClass()))
                .collect(toImmutableMap(t -> t.getClass().getName(), Function.identity()));

        log.info("found {} tasks annotated by @CeleryTask", tasks.size());
        if (log.isDebugEnabled()) {
            final String taskNames = tasks.keySet().stream()
                    .sorted().collect(Collectors.joining("\n"));
            log.debug("the found @CeleryTasks are:\n{}", taskNames);
        }
    }

    private static boolean validateTaskClass(final Class<?> taskClass) {
        if (taskClass.isAnnotationPresent(CeleryTask.class)) {
            return true;
        } else {
            log.error("bad Celery task object, missing @CeleryTask annotation on {}", taskClass);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getTask(final String taskName) {
        return (T) tasks.get(taskName);
    }
}
