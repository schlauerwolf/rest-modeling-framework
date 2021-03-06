package io.vrap.rmf.raml.generic.generator.php;

import com.google.common.collect.Lists;
import io.vrap.rmf.raml.model.modules.Api;
import io.vrap.rmf.raml.model.types.*;
import io.vrap.rmf.raml.model.types.util.TypesSwitch;
import org.eclipse.emf.ecore.EObject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class TypeGenModel {
    static final String TYPES = "Types";

    private final AnyType type;

    public TypeGenModel(final AnyType type) {
        this.type = type;
    }

    public Boolean getHasBuiltinParent()
    {
        return new BuiltinParentVisitor().doSwitch(type);
    }

    @Nullable
    public String getName()
    {
        return type.getName();
    }

    public TypeGenModel getParent()
    {
        return new TypeGenModel(type.getType());
    }

    @Nullable
    public String getDiscriminator()
    {
        if (type instanceof ObjectType) {
            return ((ObjectType) type).getDiscriminator();
        }
        return null;
    }

    @Nullable
    public List<TypeGenModel> getOneOf() {
        if (type instanceof UnionType) {
            return ((UnionType)type).getOneOf().stream().map(TypeGenModel::new).collect(Collectors.toList());
        }
        return null;
    }

    @Nullable
    public String getDiscriminatorValue()
    {
        if (type instanceof ObjectType) {
            return ((ObjectType) type).getDiscriminatorValue();
        }
        return null;
    }

    public Boolean getIsClass()
    {
        return new IsClassVisitor().doSwitch(type);
    }

    public ImportGenModel getImport()
    {
        final String name = (new GeneratorHelper.TypeNameVisitor()).doSwitch(type);
        return new ImportGenModel(getPackage(), name);
    }

    @Nullable
    public List<TypeGenModel> getSubTypes()
    {
        if (type.subTypes() != null) {
            return type.subTypes().stream().map(TypeGenModel::new).collect(Collectors.toList());
        }
        return null;
    }

    public List<PropertyGenModel> getTypeProperties()
    {
        if (type instanceof ObjectType) {
            ObjectType superType = (ObjectType)type.getType();
            return ((ObjectType)type).getProperties().stream()
                    .filter(property -> !(property.getName().startsWith("/") && property.getName().endsWith("/")))
                    .filter(property -> superType.getAllProperties().stream().filter(property1 -> property1.getName().equals(property.getName())).count() == 0)
                    .map(PropertyGenModel::new)
                    .collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    public List<PropertyGenModel> getNonPatternProperties()
    {
        if (type instanceof ObjectType) {
            return ((ObjectType)type).getProperties().stream()
                    .filter(property -> !(property.getName().startsWith("/") && property.getName().endsWith("/")))
                    .map(PropertyGenModel::new)
                    .collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    public List<PropertyGenModel> getPatternProperties()
    {
        if (type instanceof ObjectType) {
            return ((ObjectType)type).getProperties().stream()
                    .filter(property -> property.getName().startsWith("/") && property.getName().endsWith("/"))
                    .map(PropertyGenModel::new)
                    .collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    @Nullable
    public List<SerializerGenModel> getSerializers()
    {
        if (type instanceof ObjectType) {
            return ((ObjectType)type).getProperties().stream().map(property -> {
                    return new GeneratorHelper.SerializerVisitor(new PropertyGenModel(property)).doSwitch(property.getType());
                }).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return null;
    }

    public List<PropertyGenModel> getUnionProperties()
    {
        if (type instanceof ObjectType) {
            return ((ObjectType) type).getProperties().stream()
                    .filter(property -> !(property.getName().startsWith("/") && property.getName().endsWith("/")))
                    .filter(property -> property.getType() instanceof UnionType)
                    .map(PropertyGenModel::new)
                    .collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    public String getTypeName()
    {
        return new GeneratorHelper.TypeNameVisitor().doSwitch(type);
    }

    public String getReturnTypeName()
    {
        return new GeneratorHelper.TypeNameVisitor().doSwitch(type);
    }

    public Set<ImportGenModel> getTypeImports()
    {
        if (type instanceof ObjectType) {
            ObjectType objectType = (ObjectType)type;

            final Set<ImportGenModel> uses = objectType.getProperties().stream()
                    .filter(property -> property.getType() instanceof ObjectType || property.getType() instanceof ArrayType && ((ArrayType) property.getType()).getItems() instanceof ObjectType)
                    .filter(property -> {
                        AnyType t = property.getType() instanceof ArrayType ? ((ArrayType) property.getType()).getItems() : property.getType();
                        return !new TypeGenModel(t).getPackage().getName().equals(getPackage().getName());
                    })
                    .map(property -> {
                        AnyType t = property.getType() instanceof ArrayType ? ((ArrayType) property.getType()).getItems() : property.getType();
                        return new TypeGenModel(t).getImport();
                    })
                    .collect(Collectors.toSet());
            uses.addAll(
                    objectType.getProperties().stream()
                            .map(GeneratorHelper::getBaseProperty)
                            .filter(property -> property.getType() instanceof ObjectType || property.getType() instanceof ArrayType && ((ArrayType) property.getType()).getItems() instanceof ObjectType)
                            .filter(property -> {
                                AnyType t = property.getType() instanceof ArrayType ? ((ArrayType) property.getType()).getItems() : property.getType();
                                return !new TypeGenModel(t).getPackage().getName().equals(getPackage().getName());
                            })
                            .map(property -> new TypeGenModel(property.getType()).getImport())
                            .collect(Collectors.toSet())
            );
            if (!getHasBuiltinParent() && !getParent().getPackage().getName().equals(getPackage().getName())) {
                uses.add(getParent().getImport());
            }
            if (getDiscriminator() != null && getPackage().getHasPackage()) {
                uses.add(new ImportGenModel(new PackageGenModel(TYPES)));
            }
            return uses;
        }
        return null;
    }

    public Boolean getHasNonPatternProperties()
    {
        return getNonPatternProperties().size() > 0;
    }

    public Boolean getHasPatternProperties()
    {
        return getPatternProperties().size() > 0;
    }

    public PackageGenModel getPackage()
    {
        final AnyType t = type instanceof ArrayType ? ((ArrayType) type).getItems() : type;
        Annotation annotation = t.getAnnotation("package", true);
        return new PackageGenModel(TYPES, annotation);
    }

    @Nullable
    public List<PropertyGenModel> getIdentifiers()
    {
        final AnyType t = type instanceof ArrayType ? ((ArrayType) type).getItems() : type;
        AnyAnnotationType identifierAnnotationType = getApi().getAnnotationType("identifier");
        if (t instanceof ObjectType) {
            return  ((ObjectType)t).getAllProperties().stream()
                    .map(PropertyGenModel::new)
                    .filter(property -> property.getIdentifier() != null)
                    .collect(Collectors.toList());
        }
        return null;
    }

    public Api getApi()
    {
        return GeneratorHelper.getParent(type, Api.class);
    }

    private class BuiltinParentVisitor extends TypesSwitch<Boolean> {
        @Override
        public Boolean defaultCase(EObject object) {
            return true;
        }

        @Override
        public Boolean caseArrayType(final ArrayType arrayType) {
            final AnyType items = arrayType.getItems();
            return items != null && items.getName() != null && (items.getType() == null || BuiltinType.of(items.getName()).isPresent());
        }

        @Override
        public Boolean caseObjectType(final ObjectType objectType) {
            return objectType.getName() != null && (objectType.getType() == null || BuiltinType.of(objectType.getType().getName()).isPresent());
        }
    }

    private class IsClassVisitor extends TypesSwitch<Boolean> {
        @Override
        public Boolean defaultCase(EObject object) {
            return false;
        }

        @Override
        public Boolean caseArrayType(final ArrayType arrayType) {
            final AnyType items = arrayType.getItems();
            return items != null && items.getName() != null;
        }

        @Override
        public Boolean caseObjectType(final ObjectType objectType) {
            return objectType.getName() != null;
        }
    }
}
