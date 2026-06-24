package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.model.ValueType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.polyconf.parser.model.ConfigNode;

public final class PropertiesParser implements LenientParser {

    @Override
    public ConfigSection parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        Map<String, ConfigNode> children = new LinkedHashMap<>();

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String trimmed = raw.strip();

            if (trimmed.isEmpty() || isComment(trimmed)) {
                continue;
            }

            int sepIdx = findSeparator(trimmed);
            if (sepIdx < 0) {
                continue;
            }

            String key = trimmed.substring(0, sepIdx).strip();
            String value = trimmed.substring(sepIdx + 1).strip();

            if (key.isEmpty()) {
                continue;
            }

            children.put(key, new ConfigValue(
                    key,
                    value,
                    ValueType.STRING,
                    new Provenance(i, null, raw, 1.0),
                    ""
            ));
        }

        return new ConfigSection("", children, null, "");
    }

    private static boolean isComment(String line) {
        return line.startsWith("#") || line.startsWith("!");
    }

    private static int findSeparator(String line) {
        int eqIdx = line.indexOf('=');
        int colonIdx = line.indexOf(':');
        if (eqIdx < 0 && colonIdx < 0) {
            return -1;
        }
        if (eqIdx < 0) {
            return colonIdx;
        }
        if (colonIdx < 0) {
            return eqIdx;
        }
        return Math.min(eqIdx, colonIdx);
    }
}
