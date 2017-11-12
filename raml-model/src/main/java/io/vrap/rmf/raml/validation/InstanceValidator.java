package io.vrap.rmf.raml.validation;

import io.vrap.rmf.raml.model.facets.*;
import io.vrap.rmf.raml.model.facets.util.FacetsSwitch;
import io.vrap.rmf.raml.model.types.AnyAnnotationType;
import io.vrap.rmf.raml.model.types.AnyType;
import io.vrap.rmf.raml.model.types.ItemsFacet;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class InstanceValidator {

    public List<Diagnostic> validate(final Instance instance, final AnyType anyType) {
        return validateInternal(instance, anyType);
    }

    public List<Diagnostic> validate(final Instance instance, final AnyAnnotationType anyAnnotationType) {
        return validateInternal(instance, anyAnnotationType);
    }

    private List<Diagnostic> validateInternal(final Instance instance, final EObject type) {
        final InstanceValidatingVisitor validatingVisitor = new InstanceValidatingVisitor(type);
        final List<Diagnostic> validationResults = validatingVisitor.doSwitch(instance);
        return validationResults;
    }

    private List<Diagnostic> validateEnumFacet(final EnumFacet enumFacet, final Object value) {
        final List<Diagnostic> validationResults = new ArrayList<>();
        final Optional<Instance> enumInstance = enumFacet.getEnum().stream()
                .filter(enumValue -> enumValue.getValue().equals(value))
                .findFirst();
        if (enumFacet.getEnum().size() > 0 && !enumInstance.isPresent()) {
            validationResults.add(error("Value is not defined in enum facet", enumFacet));
        }
        return validationResults;
    }

    private Diagnostic error(final String message, final EObject eObject) {
        return new BasicDiagnostic(Diagnostic.ERROR, null, -1, message, new Object[]{eObject});
    }

    private class InstanceValidatingVisitor extends FacetsSwitch<List<Diagnostic>> {
        private final Stack<EObject> types = new Stack<>();

        public InstanceValidatingVisitor(final EObject type) {
            types.push(type);
        }

        @Override
        public List<Diagnostic> defaultCase(EObject object) {
            return Collections.emptyList();
        }

        @Override
        public List<Diagnostic> caseStringInstance(final StringInstance stringInstance) {
            final List<Diagnostic> validationResults = new ArrayList<>();

            if (typeInstanceOf(StringTypeFacet.class) && typeInstanceOf(EnumFacet.class)) {
                final String value = stringInstance.getValue();

                final StringTypeFacet stringType = (StringTypeFacet) types.peek();
                if (stringType.getMinLength() != null && value.length() < stringType.getMinLength()) {
                    validationResults.add(error("Value length < minLength", stringInstance));
                }
                if (stringType.getMaxLength() != null && value.length() > stringType.getMaxLength()) {
                    validationResults.add(error("Value length > maxLength", stringInstance));
                }
                if (stringType.getPattern() != null && !stringType.getPattern().test(value)) {
                    validationResults.add(error("Value doesn't match pattern "
                            + stringType.getPattern(), stringInstance));
                }

                final EnumFacet enumFacet = (EnumFacet) types.peek();
                validationResults.addAll(validateEnumFacet(enumFacet, value));
            } else {
                validationResults.add(error("Invalid type", stringInstance));
            }
            return validationResults;
        }


        @Override
        public List<Diagnostic> caseNumberInstance(final NumberInstance numberInstance) {
            final List<Diagnostic> validationResults = new ArrayList<>();
            if (typeInstanceOf(NumberTypeFacet.class) && typeInstanceOf(EnumFacet.class)) {
                final NumberTypeFacet numberType = (NumberTypeFacet) types.peek();
                final BigDecimal value = numberInstance.getValue();
                if (numberType.getMinimum() != null && value.compareTo(numberType.getMinimum()) < 0) {
                    validationResults.add(error("Value < minimum", numberInstance));
                }
                if (numberType.getMaximum() != null && value.compareTo(numberType.getMaximum()) > 0) {
                    validationResults.add(error("Value > maximum", numberInstance));
                }
                if (numberType.getMultipleOf() != null && value.remainder(BigDecimal.valueOf(numberType.getMultipleOf())).compareTo(BigDecimal.ZERO) != 0) {
                    validationResults.add(error("Value is not a multiple of " + numberType.getMultipleOf(), numberInstance));
                }

                final EnumFacet enumFacet = (EnumFacet) types.peek();
                validationResults.addAll(validateEnumFacet(enumFacet, value));
            } else {
                validationResults.add(error("Invalid type", numberInstance));
            }
            return validationResults;
        }

        @Override
        public List<Diagnostic> caseIntegerInstance(final IntegerInstance integerInstance) {
            final List<Diagnostic> validationResults = new ArrayList<>();
            if (typeInstanceOf(IntegerTypeFacet.class) && typeInstanceOf(EnumFacet.class)) {
                final IntegerTypeFacet integerType = (IntegerTypeFacet) types.peek();
                final Integer value = integerInstance.getValue();
                if (integerType.getMinimum() != null && value.compareTo(integerType.getMinimum()) < 0) {
                    validationResults.add(error("Value < minimum", integerInstance));
                }
                if (integerType.getMaximum() != null && value.compareTo(integerType.getMaximum()) > 0) {
                    validationResults.add(error("Value > maximum", integerInstance));
                }
                if (integerType.getMultipleOf() != null && value % integerType.getMultipleOf() != 0) {
                    validationResults.add(error("Value is not a multiple of " + integerType.getMultipleOf(), integerInstance));
                }

                final EnumFacet enumFacet = (EnumFacet) types.peek();
                validationResults.addAll(validateEnumFacet(enumFacet, value));
            } else {
                validationResults.add(error("Invalid type", integerInstance));
            }
            return validationResults;
        }

        @Override
        public List<Diagnostic> caseArrayInstance(final ArrayInstance arrayInstance) {
            final List<Diagnostic> validationResults = new ArrayList<>();
            if (typeInstanceOf(ArrayTypeFacet.class) && typeInstanceOf(ItemsFacet.class)) {
                final ArrayTypeFacet arrayType = (ArrayTypeFacet) types.peek();
                final EList<Instance> values = arrayInstance.getValue();
                if (arrayType.getMinItems() != null && values.size() < arrayType.getMinItems()) {
                    validationResults.add(error("Array size < minItems", arrayInstance));
                }
                if (arrayType.getMaxItems() != null && values.size() > arrayType.getMaxItems()) {
                    validationResults.add(error("Array size > maxItems", arrayInstance));
                }
                if (arrayType.getUniqueItems() != null && arrayType.getUniqueItems()) {
                    final Set<Object> uniqueItems = new HashSet<>();
                    // TODO this only works for primitive values, we should extend it for object instance and array instance
                    final Set<Instance> duplicateValues = values.stream()
                            .filter(value -> !uniqueItems.add(value.getValue()))
                            .collect(Collectors.toSet());
                    if (duplicateValues.size() > 0) {
                        validationResults.add(error("Array instance contains duplicate values", arrayInstance));
                    }
                }
                final ItemsFacet itemsFacet = (ItemsFacet) types.peek();
                if (itemsFacet.getItems() != null) {
                    try {
                        types.push(itemsFacet.getItems());
                        values.stream()
                                .flatMap(instance -> doSwitch(instance).stream())
                                .forEach(validationResults::add);
                    } finally {
                        types.pop();
                    }
                }
            } else {
                validationResults.add(error("Invalid type", arrayInstance));
            }
            return validationResults;
        }

        private boolean typeInstanceOf(final Class<?> clazz) {
            return !types.empty() && clazz.isInstance(types.peek());
        }
    }
}