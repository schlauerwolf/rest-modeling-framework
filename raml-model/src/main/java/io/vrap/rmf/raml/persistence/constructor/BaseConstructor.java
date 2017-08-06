package io.vrap.rmf.raml.persistence.constructor;

import io.vrap.rmf.raml.persistence.antlr.RAMLParser;

import static io.vrap.rmf.raml.model.types.TypesPackage.Literals.ANY_TYPE__DEFAULT;
import static io.vrap.rmf.raml.model.types.TypesPackage.Literals.ANY_TYPE__EXAMPLE;

public abstract class BaseConstructor extends AbstractConstructor {
    private final InstanceConstructor instanceConstructor = new InstanceConstructor();


    @Override
    public Object visitDefaultFacet(RAMLParser.DefaultFacetContext defaultFacet) {
        return instanceConstructor.withinScope(scope.with(ANY_TYPE__DEFAULT), defaultScope ->
                instanceConstructor.visitInstance(defaultFacet.instance()));
    }

    @Override
    public Object visitExampleFacet(RAMLParser.ExampleFacetContext exampleFacet) {
        return instanceConstructor.withinScope(scope.with(ANY_TYPE__EXAMPLE), exampleScope ->
                instanceConstructor.visitInstance(exampleFacet.instance()));
    }
}
