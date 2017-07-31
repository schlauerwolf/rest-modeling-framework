package io.vrap.rmf.raml.persistence.constructor

import io.vrap.rmf.raml.model.modules.Api
import io.vrap.rmf.raml.model.resources.Resource
import io.vrap.rmf.raml.model.resources.UriTemplateExpression
import io.vrap.rmf.raml.model.resources.UriTemplateLiteral
import io.vrap.rmf.raml.persistence.RamlResourceSet
import io.vrap.rmf.raml.persistence.antlr.RAMLCustomLexer
import io.vrap.rmf.raml.persistence.antlr.RAMLParser
import org.antlr.v4.runtime.CommonTokenFactory
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.TokenStream
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.resource.URIConverter
import spock.lang.Shared
import spock.lang.Specification

/**
 * Unit tests for {@link ApiConstructor}
 */
class ApiConstructorTest extends Specification {
    @Shared
    ResourceSet resourceSet = new RamlResourceSet()
    @Shared
    URI uri = URI.createURI("test.raml");

    def "simple api attributes"() {
        when:
        Api api = constructApi(
                '''\
        title: Simple API
        version: v1
        protocols:
            - http
            - https
        mediaType: application/json
        ''')

        then:
        api.title == 'Simple API'
        api.protocols == ['http', 'https']
        api.mediaType == ['application/json']
    }

    def "base uri and base uri parameters"() {
        when:
        Api api = constructApi(
                '''\
        baseUri: https://api.simple.com/{version}/api/{userId}
        baseUriParameters:
            userId: integer
        ''')

        then:
        api.baseUri.parts.size() == 4
        api.baseUri.parts[0] instanceof UriTemplateLiteral
        UriTemplateLiteral uriTemplateLiteral = api.baseUri.parts[0]
        uriTemplateLiteral.literal == 'https://api.simple.com/'

        api.baseUri.parts[1] instanceof UriTemplateExpression
        UriTemplateExpression versionTemplateExpression = api.baseUri.parts[1]
        versionTemplateExpression.variables.size() == 1
        versionTemplateExpression.variables[0] == 'version'

        api.baseUri.parts[2] instanceof UriTemplateLiteral

        api.baseUri.parts[3] instanceof UriTemplateExpression
        UriTemplateExpression userIdTemplateExpression = api.baseUri.parts[3]
        userIdTemplateExpression.variables.size() == 1
        userIdTemplateExpression.variables[0] == 'userId'

        api.baseUriParameters.size() == 1
        api.baseUriParameters[0].name == 'userId'
        api.baseUriParameters[0].type.name == 'integer'
    }

    def "simple resource attributes"() {
        when:
        Api api = constructApi(
                '''\
        /user:
            description: User endpoint
            displayName: Users
        ''')

        then:
        api.resources.size() == 1
        Resource resource = api.resources[0]
        resource.relativeUri.parts.size() == 1
        resource.relativeUri.parts[0] instanceof UriTemplateLiteral
        resource.description == 'User endpoint'
        resource.displayName == 'Users'
    }

    def "simple uri parameters"() {
        when:
        Api api = constructApi(
                '''\
        /user/{userId}:
            uriParameters:
                userId: integer
        ''')

        then:
        api.resources.size() == 1
        Resource resource = api.resources[0]
        resource.relativeUri.parts.size() == 2
        resource.relativeUri.parts[0] instanceof UriTemplateLiteral
        resource.relativeUri.parts[1] instanceof UriTemplateExpression
        resource.uriParameters.size() == 1
        resource.uriParameters[0].name == 'userId'
        resource.uriParameters[0].type.name == 'integer'
    }

    def "simple sub resources"() {
        when:
        Api api = constructApi(
                '''\
        /user:
            /{userId}:
                uriParameters:
                    userId: integer
        ''')

        then:
        api.resources.size() == 1
        Resource resource = api.resources[0]
        resource.relativeUri.parts.size() == 1
        resource.relativeUri.parts[0] instanceof UriTemplateLiteral
        resource.resources.size() == 1
        Resource subResource = resource.resources[0]
        subResource.relativeUri.parts.size() == 2
        subResource.relativeUri.parts[0] instanceof UriTemplateLiteral
        subResource.relativeUri.parts[1] instanceof UriTemplateExpression
        subResource.uriParameters.size() == 1
        subResource.uriParameters[0].name == 'userId'
        subResource.uriParameters[0].type.name == 'integer'
    }

    Api constructApi(String input) {
        RAMLParser parser = parser(input)
        def apiConstructor = new ApiConstructor()
        Scope scope = Scope.of(resourceSet.createResource(uri))
        return apiConstructor.construct(parser, scope)
    }

    RAMLParser parser(String input) {
        final URIConverter uriConverter = resourceSet.getURIConverter();
        def strippedInput = input.stripIndent()
        final RAMLCustomLexer lexer = new RAMLCustomLexer(strippedInput, uri, uriConverter);
        final TokenStream tokenStream = new CommonTokenStream(lexer);
        lexer.setTokenFactory(CommonTokenFactory.DEFAULT);
        new RAMLParser(tokenStream)
    }
}