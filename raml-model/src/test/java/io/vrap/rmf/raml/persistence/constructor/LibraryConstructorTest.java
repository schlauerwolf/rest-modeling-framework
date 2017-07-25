package io.vrap.rmf.raml.persistence.constructor;

import io.vrap.rmf.raml.model.facets.StringInstance;
import io.vrap.rmf.raml.model.modules.Library;
import io.vrap.rmf.raml.model.types.*;
import io.vrap.rmf.raml.persistence.RamlResourceSet;
import io.vrap.rmf.raml.persistence.ResourceFixtures;
import io.vrap.rmf.raml.persistence.antlr.RAMLParser;
import io.vrap.rmf.raml.persistence.antlr.RAMLParserFixtures;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LibraryConstructor}.
 */
public class LibraryConstructorTest implements RAMLParserFixtures, ResourceFixtures {

    @Test
    public void library() throws IOException {
        final RAMLParser parser = parserFromClasspath("/libraries/library.raml");
        final Resource resource = new RamlResourceSet().createResource(uriFromClasspath("/libraries/library.raml"));
        final Scope resourceScope = Scope.of(resource);
        final LibraryConstructor constructor = new LibraryConstructor();
        final Library library = (Library) constructor.construct(parser, resourceScope);

        assertThat(library.getUsage()).isEqualTo("Test");
        final EList<AnyType> types = library.getTypes();
        assertThat(types).hasSize(6);

        assertThat(types.get(0).getName()).isEqualTo("StringType");
        assertThat(types.get(0)).isInstanceOf(StringType.class);
        final StringType stringType = (StringType) types.get(0);
        assertThat(stringType.getMinLength()).isEqualTo(10);

        assertThat(types.get(3).getName()).isEqualTo("SuperType");
        assertThat(types.get(3)).isInstanceOf(ObjectType.class);
        final ObjectType superType = (ObjectType) types.get(3);

        assertThat(types.get(2).getName()).isEqualTo("WithProperties");
        assertThat(types.get(2)).isInstanceOf(ObjectType.class);
        final ObjectType objectType = (ObjectType) types.get(2);
        assertThat(objectType.getProperties()).hasSize(2);
        assertThat(objectType.getProperty("super")).isNotNull();
        assertThat(objectType.getProperty("super").getType()).isEqualTo(superType);

        assertThat(types.get(4).getName()).isEqualTo("SubType");
        assertThat(types.get(4)).isInstanceOf(ObjectType.class);
        final ObjectType subType = (ObjectType) types.get(4);
        assertThat(subType.getType()).isEqualTo(superType);

        assertThat(types.get(5).getName()).isEqualTo("Enum");
        assertThat(types.get(5)).isInstanceOf(StringType.class);
        final StringType enumType = (StringType) types.get(5);
        assertThat(enumType.getEnum()).containsExactly("v1", "v2");
    }

    @Test
    public void generatorAnnotations() throws IOException {
        final RAMLParser parser = parserFromClasspath("/libraries/generator-annotations.raml");
        final Resource resource = new RamlResourceSet().createResource(uriFromClasspath("/libraries/generator-annotations.raml"));
        final Scope resourceScope = Scope.of(resource);
        final LibraryConstructor constructor = new LibraryConstructor();
        final Library library = (Library) constructor.construct(parser, resourceScope);

        assertThat(library.getUsage()).isEqualTo("Defines generator annotations.");
        final EList<AnyType> types = library.getTypes();
        assertThat(types).hasSize(0);

        final EList<AnyAnnotationType> annotationTypes = library.getAnnotationTypes();
        assertThat(annotationTypes).hasSize(1);

        assertThat(annotationTypes.get(0)).isInstanceOf(StringAnnotationType.class);
        final StringAnnotationType stringAnnotationType = (StringAnnotationType) annotationTypes.get(0);
        assertThat(stringAnnotationType.getName()).isEqualTo("package");
        assertThat(stringAnnotationType.getAllowedTargets()).containsExactly(AnnotationTarget.LIBRARY);
    }

    @Test
    public void annotations() throws IOException {
        final RAMLParser parser = parserFromClasspath("/annotations/annotations.raml");
        final Resource resource = new RamlResourceSet().createResource(uriFromClasspath("/annotations/annotations.raml"));
        final Scope resourceScope = Scope.of(resource);
        final LibraryConstructor constructor = new LibraryConstructor();
        final Library library = (Library) constructor.construct(parser, resourceScope);

        final EList<AnyAnnotationType> annotationTypes = library.getAnnotationTypes();
        assertThat(annotationTypes).hasSize(2);
        assertThat(annotationTypes.get(0)).isInstanceOf(StringAnnotationType.class);
        final StringAnnotationType stringAnnotationType = (StringAnnotationType) annotationTypes.get(0);

        assertThat(library.getAnnotations()).hasSize(1);
        final Annotation annotation = library.getAnnotations().get(0);
        assertThat(annotation.getType()).isEqualTo(stringAnnotationType);
        assertThat(annotation.getValue()).isInstanceOf(StringInstance.class);
        final StringInstance annotationValue = (StringInstance) annotation.getValue();
        assertThat(annotationValue.getValue()).isEqualTo("package");

        final EList<AnyType> types = library.getTypes();
        assertThat(types).hasSize(1);
        assertThat(types.get(0)).isInstanceOf(ObjectType.class);

        final ObjectType annotatedType = (ObjectType) types.get(0);
        assertThat(annotatedType.getAnnotations()).hasSize(1);
        final Property defaultProperty = annotatedType.getProperty("default");
        assertThat(defaultProperty).isNotNull();
        assertThat(defaultProperty.getAnnotations()).hasSize(1);
    }

    @Test
    public void propertiesInlineTypeDeclaration() throws IOException {
        final RAMLParser parser = parserFromClasspath("/properties/inline-type-declaration.raml");
        final Resource resource = new RamlResourceSet().createResource(uriFromClasspath("/properties/inline-type-declaration.raml"));
        final Scope resourceScope = Scope.of(resource);
        final LibraryConstructor constructor = new LibraryConstructor();
        final Library library = (Library) constructor.construct(parser, resourceScope);

        final EList<AnyType> types = library.getTypes();
        assertThat(types).hasSize(1);
        assertThat(types.get(0)).isInstanceOf(ObjectType.class);
        final ObjectType objectType = (ObjectType) types.get(0);

        final Property nameProperty = objectType.getProperty("name");
        assertThat(nameProperty).isNotNull();
        assertThat(nameProperty.getType()).isInstanceOf(StringType.class);
        final StringType inlineStringType = (StringType) nameProperty.getType();
        assertThat(inlineStringType.getName()).isNull();
        assertThat(inlineStringType.getMinLength()).isEqualTo(10);
    }

    @Test
    public void dataTypeInclude() throws IOException {
        final RAMLParser parser = parserFromClasspath("/data-type-include.raml");
        final Resource resource = new RamlResourceSet().createResource(uriFromClasspath("/data-type-include.raml"));
        final Scope resourceScope = Scope.of(resource);
        final LibraryConstructor constructor = new LibraryConstructor();
        final Library library = (Library) constructor.construct(parser, resourceScope);

        final EList<AnyType> types = library.getTypes();

        assertThat(types).hasSize(1);
        assertThat(types.get(0)).isInstanceOf(ObjectType.class);

        final ObjectType personType = (ObjectType) types.get(0);

        assertThat(personType.getName()).isEqualTo("Person");
        assertThat(personType.getDisplayName()).isEqualTo("Person");
        assertThat(personType.getProperties()).hasSize(1);

        final Property ageProperty = personType.getProperties().get(0);

        assertThat(ageProperty.getName()).isEqualTo("age");
        assertThat(ageProperty.getType().getName()).isEqualTo("integer");
    }

}
