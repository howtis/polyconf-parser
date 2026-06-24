package com.polyconf.parser.compare;

import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.ValueType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ResultDivergence {

    public enum Level {
        IDENTICAL,
        TYPE_ONLY,
        STRUCTURAL
    }

    private ResultDivergence() {
    }

    public static Level compare(ConfigSection first, ConfigSection second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("sections must not be null");
        }

        Map<String, ConfigValue> firstKeys = collectValues(first);
        Map<String, ConfigValue> secondKeys = collectValues(second);

        if (!firstKeys.keySet().equals(secondKeys.keySet())) {
            return Level.STRUCTURAL;
        }

        for (Map.Entry<String, ConfigValue> entry : firstKeys.entrySet()) {
            String key = entry.getKey();
            ConfigValue v1 = entry.getValue();
            ConfigValue v2 = secondKeys.get(key);

            if (!stringOf(v1).equals(stringOf(v2))) {
                return Level.STRUCTURAL;
            }

            if (v1.type() != v2.type()) {
                return Level.TYPE_ONLY;
            }
        }

        return Level.IDENTICAL;
    }

    private static Map<String, ConfigValue> collectValues(ConfigSection section) {
        Map<String, ConfigValue> result = new LinkedHashMap<>();
        collect(section, "", result);
        return result;
    }

    private static void collect(ConfigNode node, String prefix, Map<String, ConfigValue> result) {
        if (node instanceof ConfigValue value) {
            String key = prefix.isEmpty() ? value.key() : prefix + "." + value.key();
            result.put(key, value);
        } else if (node instanceof ConfigSection section) {
            String keyPrefix = prefix.isEmpty() ? section.key() : prefix + "." + section.key();
            for (ConfigNode child : section.children().values()) {
                collect(child, keyPrefix, result);
            }
        }
    }

    private static String stringOf(ConfigValue value) {
        if (value.type() == ValueType.NULL || value.rawValue() == null) {
            return "";
        }
        return value.rawValue().toString();
    }
}
