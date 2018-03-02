package com.geneea.celery;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;

public abstract class CeleryTaskProxy {

    @Autowired
    protected Celery client;

    protected abstract Class<?> getTaskClass();

    public static DynamicType.Unloaded<? extends CeleryTaskProxy> createFor(final Class<?> taskClass) {
        DynamicType.Builder<CeleryTaskProxy> builder = new ByteBuddy()
                .subclass(CeleryTaskProxy.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_PUBLIC)
                .name(taskClass.getName() + "Proxy")
                .modifiers(Modifier.PUBLIC & Modifier.FINAL)
                .annotateType(AnnotationDescription.Builder.ofType(Component.class).build());

        builder = builder
                .method(ElementMatchers.named("getTaskClass"))
                .intercept(FixedValue.value(taskClass));

        for (final Method m : taskClass.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())) {
                Type taskReturnType = m.getGenericReturnType();
                taskReturnType = PRIMITIVE_TO_BOXED.getOrDefault(taskReturnType, taskReturnType);
                DynamicType.Builder.MethodDefinition.ParameterDefinition<CeleryTaskProxy> taskMethod =
                        builder.defineMethod(
                                m.getName(),
                                TypeDescription.Generic.Builder.parameterizedType(
                                        ListenableFuture.class, taskReturnType
                                ).build(),
                                Modifier.PUBLIC & Modifier.FINAL
                        );
                for (final Parameter taskParam : m.getParameters()) {
                    taskMethod = taskMethod.withParameter(
                            taskParam.getParameterizedType(),
                            taskParam.getName()
                    );
                }
                builder = taskMethod
                        .throwing(m.getGenericExceptionTypes())
                        .throwing(new TypeDescription.ForLoadedType(IOException.class))
                        .intercept(MethodDelegation.to(SubmitInterceptor.class));
            }
        }

        return builder.make();
    }

    public static Class<? extends CeleryTaskProxy> createAndLoadFor(final Class<?> taskClass) {
        return createFor(taskClass)
                .load(taskClass.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
    }

    public static void createAndWriteFor(final Class<?> taskClass, final BiConsumer<String, byte[]> writer) {
        final DynamicType.Unloaded<? extends CeleryTaskProxy> dynamic = createFor(taskClass);
        writer.accept(dynamic.getTypeDescription().getActualName(), dynamic.getBytes());
    }

    public interface SubmitInterceptor {

        @RuntimeType
        static ListenableFuture<?> submit(
                final @This CeleryTaskProxy taskProxy,
                final @Origin Method method,
                final @AllArguments Object[] allArguments
        ) throws IOException {
            return taskProxy.client.submit(
                    taskProxy.getTaskClass(),
                    method.getName(),
                    allArguments
            );
        }
    }

    private static final ImmutableMap<Type, Type> PRIMITIVE_TO_BOXED = ImmutableMap.<Type, Type>builder()
            .put(boolean.class, Boolean.class)
            .put(byte.class, Byte.class)
            .put(char.class, Character.class)
            .put(short.class, Short.class)
            .put(int.class, Integer.class)
            .put(long.class, Long.class)
            .put(float.class, Float.class)
            .put(double.class, Double.class)
            .put(void.class, Void.class)
            .build();
}
