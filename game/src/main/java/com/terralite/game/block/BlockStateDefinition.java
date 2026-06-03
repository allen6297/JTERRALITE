package com.terralite.game.block;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record BlockStateDefinition(Map<String, List<String>> properties, Map<String, String> defaultValues) {
    public static final BlockStateDefinition EMPTY = new BlockStateDefinition(Map.of(), Map.of());

    public BlockStateDefinition {
        Map<String, List<String>> copiedProperties = copyProperties(properties);
        Map<String, String> copiedDefaultValues = Map.copyOf(Objects.requireNonNull(defaultValues, "defaultValues"));
        copiedProperties.forEach(BlockStateDefinition::validateProperty);
        copiedDefaultValues.forEach((property, value) -> {
            List<String> allowedValues = copiedProperties.get(property);
            if (allowedValues == null) {
                throw new IllegalArgumentException("Default block state references undeclared property: " + property);
            }
            if (!allowedValues.contains(value)) {
                throw new IllegalArgumentException(
                        "Default block state value " + value + " is not allowed for property " + property
                );
            }
        });
        properties = copiedProperties;
        defaultValues = copiedDefaultValues;
    }

    public boolean isAllowed(String property, String value) {
        List<String> allowedValues = properties.get(Objects.requireNonNull(property, "property"));
        return allowedValues != null && allowedValues.contains(Objects.requireNonNull(value, "value"));
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    private static void validateProperty(String property, List<String> values) {
        if (property == null || property.isBlank()) {
            throw new IllegalArgumentException("Block state property cannot be blank");
        }
        List<String> copy = List.copyOf(Objects.requireNonNull(values, "values"));
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("Block state property must define at least one value: " + property);
        }
        if (copy.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("Block state property values cannot be blank: " + property);
        }
    }

    private static Map<String, List<String>> copyProperties(Map<String, List<String>> properties) {
        LinkedHashMap<String, List<String>> copied = new LinkedHashMap<>();
        Objects.requireNonNull(properties, "properties")
                .forEach((property, values) -> copied.put(property, List.copyOf(Objects.requireNonNull(values, "values"))));
        return Map.copyOf(copied);
    }
}
