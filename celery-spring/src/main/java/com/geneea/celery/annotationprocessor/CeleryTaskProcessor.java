package com.geneea.celery.annotationprocessor;

import com.geneea.celery.CeleryTask;
import com.geneea.celery.CeleryTaskProxy;

import com.google.auto.service.AutoService;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.springframework.stereotype.Component;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementScanner8;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Internal, annotation processor that can automatically generate special proxies for
 * the classes annotated by {@link CeleryTask @CeleryTask}.
 *
 * @see CeleryTaskProxy
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.geneea.celery.CeleryTask")
public class CeleryTaskProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver() || roundEnv.errorRaised()) {
            return true;
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(CeleryTask.class)) {
            try {
                processElement(element);
            } catch (Throwable e) {
                // we should not allow any exceptions to propagate to the compiler
                printError("unexpected error while processing an element", e, element);
            }
        }
        return true;
    }

    private void printError(final String msg, final Throwable e, final Element element) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Error in processing @CeleryTask: ").append(msg);
        if (e != null) {
            final StringWriter stackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTrace));
            sb.append("\n").append(stackTrace);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, sb.toString(), element);
    }

    /**
     * Process the element annotated by {@link CeleryTask @CeleryTask}.
     */
    private void processElement(final Element element) {
        if (element.getKind() != ElementKind.CLASS && element.getKind() != ElementKind.INTERFACE) {
            printError("the element has to be class or interface", null, element);
            return;
        }
        if (!(element instanceof TypeElement)) {
            printError("the element needs to be instance of TypeElement", null, element);
            return;
        }
        if (((TypeElement) element).getNestingKind().isNested()) {
            printError("the element can not be nested class", null, element);
            return;
        }

        final TypeElement taskType = (TypeElement) element;
        final TypeSpec proxySpec = createProxyFor(taskType);
        writeClass(proxySpec, taskType);
    }

    /**
     * Writes the given spec as a class-file into the output source folder.
     */
    private void writeClass(final TypeSpec spec, final TypeElement element) {
        final PackageElement pkg = (PackageElement) element.getEnclosingElement();
        final String packageName = pkg.getQualifiedName().toString();
        try {
            JavaFile.builder(packageName, spec).build()
                    .writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            printError("could not write the generated class file", e, element);
        }
    }

    /**
     * Creates new spec of the proxied class for the CeleryTask class (as an element).
     * The generated class will look like:
     * <pre><code>
     *     &#64;Component
     *     public class MyTaskProxy extends CeleryTaskProxy {
     *
     *         &#64;Override
     *         public Class&lt;MyTask> getTaskClass() {
     *             return MyTask.class;
     *         }
     *
     *         // .... proxied methods ....
     *     }
     * </code></pre>
     *
     * @see AddProxiedMethods
     */
    static TypeSpec createProxyFor(final TypeElement taskType) {
        final TypeSpec.Builder builder = TypeSpec
                .classBuilder(taskType.getSimpleName() + "Proxy")
                .superclass(CeleryTaskProxy.class)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class);

        // override the getter for the proxied task type
        builder.addMethod(MethodSpec
                .methodBuilder("getTaskClass")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(
                        ClassName.get(Class.class),
                        TypeName.get(taskType.asType())
                ))
                .addStatement("return $T.class", taskType)
                .build()
        );

        // add auto-generated proxied methods
        taskType.accept(new AddProxiedMethods(), builder);

        return builder.build();
    }

    /**
     * A visitor which adds auto-generated proxied methods to the {@code TypeSpec.Builder}
     * for each {@code public} method of the traversed element. The methods will look like:
     * <pre><code>
     *     public final ListenableFuture&lt;MyRetType> myMethodName(
     *             MyParam1 p1, MyParam2 p2, ...
     *     ) throws IOException {
     *         return submit("myMethodName", new Object[]{p1, p2, ...});
     *     }
     * </code></pre>
     */
    static final class AddProxiedMethods extends ElementScanner8<Void, TypeSpec.Builder> {

        static boolean shouldBeProxied(final ExecutableElement method) {
            final Set<Modifier> modifiers = method.getModifiers();
            return method.getKind() == ElementKind.METHOD
                    && modifiers.contains(Modifier.PUBLIC)
                    && !modifiers.contains(Modifier.STATIC);
        }

        @Override
        public Void visitExecutable(final ExecutableElement method, final TypeSpec.Builder builder) {
            if (shouldBeProxied(method)) {
                final MethodSpec.Builder methodBuilder = MethodSpec
                        .methodBuilder(method.getSimpleName().toString())
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addException(IOException.class)
                        .returns(ParameterizedTypeName.get(
                                ClassName.get(ListenableFuture.class),
                                TypeName.get(method.getReturnType()).box()
                        ));

                final List<ParameterSpec> params = method.getParameters().stream()
                        .map(param -> ParameterSpec.builder(
                                TypeName.get(param.asType()),
                                param.getSimpleName().toString()
                        ).build())
                        .collect(Collectors.toList());
                methodBuilder.addParameters(params);

                final String paramVars = IntStream.range(0, params.size())
                        .mapToObj(i -> "$N").collect(Collectors.joining(","));
                methodBuilder.addStatement(
                        "return submit($S, new Object[]{" + paramVars + "})",
                        Lists.asList(method.getSimpleName(), params.toArray()).toArray()
                );

                builder.addMethod(methodBuilder.build());
            }
            return super.visitExecutable(method, builder);
        }
    }
}
