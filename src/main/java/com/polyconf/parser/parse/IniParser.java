package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.model.ValueType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IniParser implements LenientParser {

    @Override
    public ConfigSection parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        Map<String, ConfigNode> sections = new LinkedHashMap<>();
        Map<String, ConfigNode> globalKeys = new LinkedHashMap<>();
        Map<String, ConfigNode> current = globalKeys;
        String currentSectionKey = "";

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String trimmed = raw.strip();

            if (trimmed.isEmpty() || isComment(trimmed)) {
                continue;
            }

            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                String sectionName = trimmed.substring(1, trimmed.length() - 1).strip();
                if (!sectionName.isEmpty()) {
                    if (!globalKeys.isEmpty() && sections.isEmpty()) {
                        for (Map.Entry<String, ConfigNode> entry : globalKeys.entrySet()) {
                            sections.put(entry.getKey(), entry.getValue());
                        }
                    }
                    ConfigSection section = new ConfigSection(sectionName, null, "");
                    sections.put(sectionName, section);
                    current = section.children();
                    currentSectionKey = sectionName;
                }
                continue;
            }

            int eqIdx = trimmed.indexOf('=');
            if (eqIdx < 0) {
                continue;
            }

            String key = trimmed.substring(0, eqIdx).strip();
            String value = trimmed.substring(eqIdx + 1).strip();

            if (key.isEmpty()) {
                continue;
            }

            current.put(key, new ConfigValue(
                    key,
                    value,
                    ValueType.STRING,
                    new Provenance(i, null, raw, 1.0),
                    ""
            ));
        }

        if (!sections.isEmpty()) {
            return new ConfigSection("", sections, null, "");
        }

        return new ConfigSection("", globalKeys, null, "");
    }

    private static boolean isComment(String line) {
        return line.startsWith(";") || line.startsWith("#");
    }
}
