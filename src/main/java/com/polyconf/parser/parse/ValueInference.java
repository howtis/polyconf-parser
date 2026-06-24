package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.model.ValueType;

public final class ValueInference {

    private ValueInference() {
    }

    public static ConfigValue createValue(String key, String raw, Provenance provenance) {
        if (raw.isEmpty()) {
            return new ConfigValue(key, "", ValueType.STRING, provenance, "");
        }
        // Boolean
        if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) {
            return new ConfigValue(key, Boolean.parseBoolean(raw), ValueType.BOOLEAN, provenance, "");
        }
        // Integer
        try {
            return new ConfigValue(key, Long.parseLong(raw), ValueType.INTEGER, provenance, "");
        } catch (NumberFormatException ignored) {
        }
        // Float
        try {
            return new ConfigValue(key, Double.parseDouble(raw), ValueType.FLOAT, provenance, "");
        } catch (NumberFormatException ignored) {
        }
        return new ConfigValue(key, raw, ValueType.STRING, provenance, "");
    }
}
