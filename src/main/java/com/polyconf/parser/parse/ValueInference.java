package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.model.ValueType;

public final class ValueInference {

    private ValueInference() {
    }

    public static ConfigValue createValue(String key, String raw, Provenance provenance) {
        return new ConfigValue(key, raw, ValueType.STRING, provenance, "");
    }

    public static ConfigValue createValue(String key, Object value, Provenance provenance) {
        if (value == null) {
            return new ConfigValue(key, null, ValueType.NULL, provenance, "");
        }
        return new ConfigValue(key, value.toString(), ValueType.STRING, provenance, "");
    }
}
