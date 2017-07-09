package io.vrap.rmf.raml.persistence.constructor;

import io.vrap.rmf.raml.model.types.Api;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;

import java.util.List;
import java.util.Optional;

import static io.vrap.rmf.raml.model.types.TypesPackage.Literals.*;

/**
 * Constructs an api {@link Api} instance.
 */
public class ApiConstructor extends Constructor<MappingNode> {

    public ApiConstructor() {
        API.getEAllAttributes().forEach(this::addConstructor);
        addConstructor(TYPE_CONTAINER__USES, new LibraryUsesConstructor());
        addConstructor(TYPE_CONTAINER__TYPES,
                new TypeDeclarationsConstructor());
        addConstructor(TYPE_CONTAINER__ANNOTATION_TYPES,
                new TypeDeclarationsConstructor());
    }

    @Override
    public Object apply(final MappingNode node, final Scope resourceScope) {
        final EObject rootObject = EcoreUtil.create(API);
        final Resource resource = resourceScope.getResource();
        resource.getContents().add(rootObject);

        final Scope rootScope = resourceScope.with(rootObject);

        final List<NodeTuple> nodeTuples = node.getValue();
        nodeTuples.stream()
                .forEach(nodeTuple -> constructFeature(nodeTuple, rootScope));

        return rootObject;
    }

    protected void constructFeature(final NodeTuple featureNodeTuple, final Scope libraryScope) {
        final Optional<Constructor<NodeTuple>> constructor = constructor(API, featureNodeTuple);

        if (constructor.isPresent()) {
            final Optional<EStructuralFeature> feature = feature(API, featureNodeTuple);
            final Scope featureScope = libraryScope.with(feature.get());

            constructor.get().apply(featureNodeTuple, featureScope);
        } else {
            libraryScope.addError("No constructor for {0} found", featureNodeTuple.getKeyNode());
        }
    }
}
