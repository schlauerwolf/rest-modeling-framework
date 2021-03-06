package io.vrap.rmf.raml.generic.generator.java;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.squareup.javapoet.*;
import io.vrap.rmf.raml.model.facets.StringInstance;
import io.vrap.rmf.raml.model.modules.TypeContainer;
import io.vrap.rmf.raml.model.types.*;
import io.vrap.rmf.raml.model.types.util.TypesSwitch;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * This class generates java interfaces from a type container {@link TypeContainer}.
 * It uses javapoet to generate the java source code.
 */
public class TypesGenerator {
    private String packageName;
    private final File generateTo;

    private Map<String, String> customTypeMapping = Collections.singletonMap("LocalizedString", "io.sphere.sdk.models.LocalizedString");
    private TypeMappingVisitor typeMappingVisitor = new TypeMappingVisitor(customTypeMapping);
    private PropertyGeneratingVisitor propertyGeneratingVisitor = new PropertyGeneratingVisitor();
    private TypeGeneratingVisitor typeGeneratingVisitor = new TypeGeneratingVisitor();

    /**
     * Creates a new types generator.
     *
     * @param packageName  the package name
     * @param genSourceDir the source dir
     */
    public TypesGenerator(final String packageName, final File genSourceDir) {
        this.packageName = packageName;
        this.generateTo = genSourceDir;
    }

    /**
     * Generates java source code for the types of the given type container.
     *
     * @param types the type container
     */
    public void generate(final List<AnyType> types) {
        final List<TypeSpec> typeSpecs = types.stream()
                .filter(anyType -> !customTypeMapping.containsKey(anyType.getName()))
                .map(typeGeneratingVisitor::doSwitch)
                .collect(Collectors.toList());

        typeSpecs.stream()
                .filter(Objects::nonNull)
                .forEach(this::generateFile);
    }

    private void generateFile(final TypeSpec typeSpec) {
        final JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                .build();
        try {
            javaFile.writeTo(generateTo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class TypeGeneratingVisitor extends TypesSwitch<TypeSpec> {

        @Override
        public TypeSpec caseStringType(final StringType stringType) {
            if (stringType.getEnum().isEmpty()) {
                return null;
            } else {
                final TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(stringType.getName());
                enumBuilder.addModifiers(Modifier.PUBLIC);

                final Converter<String, String> enumCamelConverter = CaseFormat.LOWER_CAMEL
                        .converterTo(CaseFormat.UPPER_UNDERSCORE);
                final Converter<String, String> enumHyphenConverter = CaseFormat.LOWER_HYPHEN
                        .converterTo(CaseFormat.UPPER_UNDERSCORE);

                final List<String> enumStringValues = stringType.getEnum().stream()
                        .filter(StringInstance.class::isInstance)
                        .map(StringInstance.class::cast)
                        .map(StringInstance::getValue)
                        .collect(Collectors.toList());

                for (final String enumValue : enumStringValues) {

                    final String enumLiteral = enumValue.contains("-") ?
                            enumHyphenConverter.convert(enumValue) :
                            enumCamelConverter.convert(enumValue);
                    enumBuilder.addEnumConstant(enumLiteral);
                }
                return enumBuilder.build();
            }
        }

        @Override
        public TypeSpec caseObjectType(final ObjectType objectType) {
            final TypeSpec.Builder interfaceBuilder;
            if (objectType.getName() == null) {
                interfaceBuilder = null;
            } else {
                interfaceBuilder = TypeSpec.interfaceBuilder(objectType.getName());
                interfaceBuilder.addModifiers(Modifier.PUBLIC);


                final List<ObjectType> subTypes = objectType.subTypes().stream()
                        .filter(ObjectType.class::isInstance)
                        .map(ObjectType.class::cast)
                        .filter(subType -> subType.getDiscriminatorValue() != null)
                        .collect(Collectors.toList());

                if (objectType.getDiscriminator() != null) {
                    interfaceBuilder.addAnnotation(AnnotationSpec.builder(JsonTypeInfo.class)
                            .addMember("use", "$L", "JsonTypeInfo.Id.NAME")
                            .addMember("include", "$L", "JsonTypeInfo.As.PROPERTY")
                            .addMember("property", "$S", objectType.getDiscriminator())
                            .addMember("visible", "$L", true)
                            .build());
                    final AnnotationSpec.Builder jsonSubTypesBuilder = AnnotationSpec.builder(JsonSubTypes.class);

                    for (final ObjectType subType : subTypes) {
                        jsonSubTypesBuilder.addMember("value", "$L",
                                AnnotationSpec.builder(JsonSubTypes.Type.class)
                                        .addMember("value", "$T.class", ClassName.get(packageName, subType.getName()))
                                        .addMember("name", "$S", subType.getDiscriminatorValue())
                                        .build());
                    }
                    interfaceBuilder.addAnnotation(jsonSubTypesBuilder.build());
                }
                final TypeName typeName = typeMappingVisitor.doSwitch(objectType.getType());
                final String superTypeName = objectType.getType().getName();
                if (!BuiltinType.of(superTypeName).isPresent()) {
                    interfaceBuilder.addSuperinterface(typeName);
                }
                final List<MethodSpec> getterMethods = objectType.getProperties().stream()
                        .filter(property -> !property.getName().startsWith("/"))
                        .filter(property -> !subTypeHasProperty(objectType, property))
                        .map(propertyGeneratingVisitor::caseProperty)
                        .collect(Collectors.toList());

                interfaceBuilder.addMethods(getterMethods);
            }

            return interfaceBuilder.build();
        }
    }

    private boolean subTypeHasProperty(final ObjectType objectType, final Property property) {
        final List<ObjectType> subTypes = objectType.subTypes().stream()
                .filter(ObjectType.class::isInstance).map(ObjectType.class::cast)
                .collect(Collectors.toList());
        for (final ObjectType subType : subTypes) {
            if (subType.getProperty(property.getName()) != null) {
                return true;
            }
        }
        return false;
    }

    private class PropertyGeneratingVisitor extends TypesSwitch<MethodSpec> {
        @Override
        public MethodSpec caseProperty(final Property property) {
            final String getterName = "get" + capitalize(property.getName());
            final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(getterName);
            methodBuilder.addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC);

            final TypeName propertyTypeName = typeMappingVisitor.doSwitch(property.getType());
            methodBuilder.returns(propertyTypeName);

            return methodBuilder.build();
        }
    }

    private class TypeMappingVisitor extends TypesSwitch<TypeName> {
        private final Map<String, String> customMapping;

        public TypeMappingVisitor(Map<String, String> customMapping) {
            this.customMapping = customMapping;
        }

        @Override
        public TypeName caseAnyType(final AnyType anyType) {
            return ClassName.get(Object.class);
        }

        @Override
        public TypeName caseStringType(final StringType stringType) {
            return stringType.getName() == null || stringType.getEnum().isEmpty() ?
                    ClassName.get(String.class) :
                    ClassName.get(packageName, stringType.getName());
        }

        @Override
        public TypeName caseBooleanType(final BooleanType booleanType) {
            return ClassName.get(Boolean.class);
        }

        @Override
        public TypeName caseNumberType(final NumberType numberType) {
            return ClassName.get(Number.class);
        }

        @Override
        public TypeName caseIntegerType(final IntegerType integerType) {
            return ClassName.get(Integer.class);
        }

        @Override
        public TypeName caseObjectType(final ObjectType objectType) {
            final String name = objectType.getName();
            return customMapping.containsKey(name) ?
                ClassName.bestGuess(customMapping.get(name)) :
                    BuiltinType.OBJECT.getName().equals(name) ?
                            ClassName.get(Object.class) :
                            ClassName.get(packageName, name);
        }

        @Override
        public TypeName caseDateTimeType(final DateTimeType object) {
            return ClassName.get(ZonedDateTime.class);
        }

        @Override
        public TypeName caseArrayType(final ArrayType arrayType) {
            final AnyType items = arrayType.getItems();
            final TypeName itemTypeName = items != null ?
                    doSwitch(items) :
                    ClassName.get(Object.class);

            final ClassName listClassName = ClassName.get(List.class);
            return ParameterizedTypeName.get(listClassName, itemTypeName);
        }
    }
}
