package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.model.ValueType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.polyconf.parser.model.ConfigNode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DotenvParser implements LenientParser {

    private static final Pattern DOTENV_LINE = Pattern.compile(
            "^(?:export\\s+)?([A-Za-z_]\\w*)\\s*=\\s*(.*)$");

    @Override
    public ConfigSection parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        Map<String, ConfigNode> children = new LinkedHashMap<>();

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String trimmed = raw.strip();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            Matcher m = DOTENV_LINE.matcher(trimmed);
            if (!m.matches()) {
                continue;
            }

            String key = m.group(1);
            String value = unquote(m.group(2).strip());

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

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
