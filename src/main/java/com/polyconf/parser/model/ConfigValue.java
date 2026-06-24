package com.polyconf.parser.model;

import java.util.Optional;

public record ConfigValue(
        String key,
        Object rawValue,
        ValueType type,
        Provenance provenance,
        String path
) implements ConfigNode {

    public Optional<String> asString() {
        if (type == ValueType.NULL || rawValue == null) {
            return Optional.empty();
        }
        return Optional.of(rawValue.toString());
    }

    public Optional<Integer> asInt() {
        if (type == ValueType.INTEGER && rawValue instanceof Number num) {
            return Optional.of(num.intValue());
        }
        if (type == ValueType.FLOAT && rawValue instanceof Number num) {
            return Optional.of(num.intValue());
        }
        if (type == ValueType.STRING && rawValue != null) {
            try {
                return Optional.of(Integer.parseInt(rawValue.toString()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public Optional<Boolean> asBool() {
        if (type == ValueType.BOOLEAN && rawValue instanceof Boolean b) {
            return Optional.of(b);
        }
        if (type == ValueType.STRING && rawValue != null) {
            String s = rawValue.toString().toLowerCase();
            if ("true".equals(s)) return Optional.of(true);
            if ("false".equals(s)) return Optional.of(false);
        }
        return Optional.empty();
    }

    public Optional<Double> asFloat() {
        if (type == ValueType.FLOAT && rawValue instanceof Number num) {
            return Optional.of(num.doubleValue());
        }
        if (type == ValueType.INTEGER && rawValue instanceof Number num) {
            return Optional.of(num.doubleValue());
        }
        if (type == ValueType.STRING && rawValue != null) {
            try {
                return Optional.of(Double.parseDouble(rawValue.toString()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public Object get() {
        return rawValue;
    }

    public boolean isNull() {
        return type == ValueType.NULL || rawValue == null;
    }
}
