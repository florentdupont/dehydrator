package dehydrator;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import lombok.val;

import javax.annotation.Generated;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.SourceVersion.RELEASE_8;
import static javax.lang.model.element.Modifier.*;
import static javax.lang.model.type.TypeKind.BOOLEAN;
import static javax.tools.Diagnostic.Kind.NOTE;
import static javax.tools.Diagnostic.Kind.WARNING;

@SupportedAnnotationTypes({"dehydrator.Dehydrate", "dehydrator.Dehydrates"})
@SupportedSourceVersion(RELEASE_8)

@AutoService(Processor.class)
public class DehydratedProcessor extends AbstractProcessor {

    DehydrateContext context;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for(val annotation : annotations) {
            roundEnv.getElementsAnnotatedWith(annotation).forEach(x -> generateForClass((TypeElement)x, annotation));
        }
        return true;
    }

    void generateForClass(TypeElement clazz, TypeElement annotation) {

        if("Dehydrate".equals(annotation.getSimpleName().toString())) {
            // annotation unique
            processingEnv.getMessager().printMessage(NOTE, "UNIQUE" + clazz);
            val annotationMirror = getAnnotation(clazz, Dehydrate.class.getName());

            val annotationValues = getAnnotationValues(annotationMirror);

            context = DehydrateContext.fromMap(annotationValues);

            if(!context.of.isEmpty() && !context.exclude.isEmpty()) {
                processingEnv.getMessager().printMessage(WARNING,
                        "'exclude' can't be used together with 'of' and will be ignored.", clazz, annotationMirror);
            }
            if(!context.name.isEmpty() && !context.suffix.isEmpty()) {
                processingEnv.getMessager().printMessage(WARNING,
                        "'suffix' can't be used together with 'name' and will be ignored.", clazz, annotationMirror);
            }
            generate(clazz);

        } else if("Dehydrates".equals(annotation.getSimpleName().toString())) {
            //annotation multiple
            processingEnv.getMessager().printMessage(NOTE, "REPETED" + clazz);
            val annotationMirrors = getRepeatableAnnotations(clazz, Dehydrates.class.getName());

            annotationMirrors.forEach(annotationMirror -> {
                val annotationValues = getAnnotationValues(annotationMirror);

                context = DehydrateContext.fromMap(annotationValues);

                if(!context.of.isEmpty() && !context.exclude.isEmpty()) {
                    processingEnv.getMessager().printMessage(WARNING,
                            "'exclude' can't be used together with 'of' and will be ignored.", clazz, annotationMirror);
                }
                if(!context.name.isEmpty() && !context.suffix.isEmpty()) {
                    processingEnv.getMessager().printMessage(WARNING,
                            "'suffix' can't be used together with 'name' and will be ignored.", clazz, annotationMirror);
                }
                generate(clazz);

            });
        }
    }

    void generate(TypeElement elt) {
        val qualifiedClassName = getTargetQualifiedClassName(elt);

        processingEnv.getMessager().printMessage(NOTE, "Dehydrating " + elt.getQualifiedName(), elt);

        val fields = processingEnv.getElementUtils().getAllMembers(elt)
                .stream()
                .filter(member -> member instanceof VariableElement)
                .map(field -> (VariableElement) field)
                .filter(field -> !field.getModifiers().contains(STATIC))
                .filter(field -> !isCollection(field))
                .filter(field -> !isSerialVersionUID(field))
                .filter(this::isIncluded)
                .collect(toList());

        val javaFile = generate(elt, fields);

        try {
            val builderFile = processingEnv.getFiler().createSourceFile(qualifiedClassName);

            try (val out = new PrintWriter(builderFile.openWriter())) {
                javaFile.writeTo(out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    JavaFile generate(TypeElement clazz, List<VariableElement> fields) {

        val packageName = getTargetPackageName(clazz);
        val className = getTargetClassName(clazz);

        val generatedAnnotation = AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", "Dehydrator")
                .build();

        val classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(PUBLIC, FINAL)
                .addAnnotation(generatedAnnotation);

        // FIELDS
        fields.stream().map(f -> {
            if(isEntity(f)) {
                return FieldSpec.builder(Long.class, f.getSimpleName().toString() + "Id", PRIVATE).build();
            } else {
                return FieldSpec.builder(TypeName.get(f.asType()), f.getSimpleName().toString(), PRIVATE).build();
            }

        }).forEach(classBuilder::addField);

        // ACCESSORS
        fields.forEach(f -> {
            if(isEntity(f)) {
                classBuilder.addMethod(generateDehydratedGetter(f));
                classBuilder.addMethod(generateDehydratedSetter(f));

            } else {
                classBuilder.addMethod(generateGetter(f));
                classBuilder.addMethod(generateSetter(f));
            }
        });

        return JavaFile.builder(packageName, classBuilder.build()).build();
    }

    boolean isIncluded(VariableElement f) {


        // garde les fields de of
        if (!context.of.isEmpty()) {
            return context.of.contains(f.getSimpleName().toString());
        }

        // exclue les field de exclude
        return context.exclude.isEmpty() || !context.exclude.contains(f.getSimpleName().toString());
    }

    MethodSpec generateDehydratedSetter(VariableElement field) {
        val fieldName = field.getSimpleName().toString();
        return MethodSpec.methodBuilder("set" + capitalize(fieldName) + "Id")
                .addModifiers(PUBLIC)
                .returns(void.class)
                .addParameter(Long.class, fieldName+"Id")
                .addStatement("this.$L = $L", fieldName + "Id", fieldName + "Id")
                .build();
    }

    MethodSpec generateDehydratedGetter(VariableElement field) {
        val fieldName = field.getSimpleName().toString();
        return MethodSpec.methodBuilder( "get" + capitalize(fieldName) + "Id")
                .addModifiers(PUBLIC)
                .returns(Long.class)
                .addStatement("return this.$L", fieldName + "Id")
                .build();
    }

    MethodSpec generateGetter(VariableElement f) {
        val prefix = f.asType().getKind() == BOOLEAN ? "is" : "get";
        val getterName = prefix + capitalize(f.getSimpleName().toString());

        return MethodSpec.methodBuilder(getterName)
                .addModifiers(PUBLIC)
                .returns(TypeName.get(f.asType()))
                .addStatement("return this.$L", f.getSimpleName().toString())
                .build();
    }

    MethodSpec generateSetter(VariableElement field) {
        val fieldName = field.getSimpleName().toString();

        return MethodSpec.methodBuilder("set" + capitalize(fieldName))
                .addModifiers(PUBLIC)
                .returns(void.class)
                .addParameter(TypeName.get(field.asType()), fieldName)
                .addStatement("this.$L = $L", fieldName, fieldName)
                .build();
    }

    boolean isCollection(VariableElement f) {
        TypeElement iterable = processingEnv.getElementUtils().getTypeElement(context.excludedCollectionClass);
        return processingEnv.getTypeUtils().isAssignable(processingEnv.getTypeUtils().erasure(f.asType()), iterable.asType());
    }

    boolean isSerialVersionUID(VariableElement f) {
        return "serialVersionUID".equals(f.getSimpleName().toString());
    }

    boolean isEntity(VariableElement clazz) {
        val elt = processingEnv.getElementUtils().getTypeElement(context.parentEntityClass);
        return processingEnv.getTypeUtils().isAssignable(clazz.asType(), elt.asType() );
    }

    String getTargetClassName(TypeElement elt) {
        if(!context.name.isEmpty()) {
            return context.name;
        } else {
            return elt.getSimpleName() + context.suffix;
        }
    }

    String getTargetQualifiedClassName(TypeElement elt) {
        return getTargetPackageName(elt) + "." + getTargetClassName(elt);
    }

    String getTargetPackageName(TypeElement elt) {
        return context.targetPackage.isEmpty() ? packageOf(elt).getQualifiedName().toString() : context.targetPackage;
    }

    AnnotationMirror getAnnotation(Element element, String annotationName) {
        val annotation = processingEnv.getElementUtils().getTypeElement(annotationName);
        for (val mirror : element.getAnnotationMirrors()) {
            processingEnv.getMessager().printMessage(NOTE, "annotation " + mirror);

            if (mirror.getAnnotationType().asElement().equals(annotation)) {
                return mirror;
            }
        }
        return null;
    }

    List<AnnotationMirror> getRepeatableAnnotations(Element element, String annotationName) {
        val annotation = processingEnv.getElementUtils().getTypeElement(annotationName);

        for (val mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().asElement().equals(annotation)) {
                // mirror représente le type conteneur des annotations repeatable.
                // on récupére son contenu
                return (List<AnnotationMirror>)getAnnotationValues(mirror).get("value");
            }
        }
        return Collections.emptyList();
    }

    Map<String, Object> getAnnotationValues(AnnotationMirror annotation) {
        val annotationValues = new HashMap<String, Object>();
        for (val entry : processingEnv.getElementUtils().getElementValuesWithDefaults(annotation).entrySet()) {
            annotationValues.put(entry.getKey().getSimpleName().toString(), entry.getValue().getValue());
        }
        return annotationValues;
    }



    PackageElement packageOf(TypeElement element) {
        return ((PackageElement)element.getEnclosingElement());
    }

    String capitalize(String str) {
        return ("" + str.charAt(0)).toUpperCase() + str.substring(1, str.length());
    }


}