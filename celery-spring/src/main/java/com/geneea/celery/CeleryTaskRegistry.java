package com.geneea.celery;

import com.google.common.collect.ImmutableList;
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

    // just as a fall-back
    public CeleryTaskRegistry() {
        this(null);
    }

    @TaskQualifier
    @Autowired(required = false)
    public CeleryTaskRegistry(final List<Object> taskObjects) {
        tasks = taskObjects == null ? ImmutableMap.of() : taskObjects.stream()
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

    /**
     * @return all known <em>CeleryTask</em> instances
     */
    public ImmutableList<?> knownTasks() {
        return ImmutableList.copyOf(tasks.values());
    }

    /**
     * Finds a task by its full class-name, e.g. {@code org.example.MyTask}.
     * @param taskName the task name (full class-name)
     * @return the specified task object if available, otherwise {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T getTask(final String taskName) {
        return (T) tasks.get(taskName);
    }

    /**
     * Finds a task by its class-name, e.g. {@code org.example.MyTask}.
     * @param taskClass the task class-name
     * @return the specified task object if available, otherwise {@code null}
     */
    public <T> T getTask(final Class<T> taskClass) {
        return getTask(taskClass.getName());
    }
}
