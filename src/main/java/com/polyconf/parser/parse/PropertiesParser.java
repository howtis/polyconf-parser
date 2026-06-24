package com.polyconf.parser.parse;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.model.ValueType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.polyconf.parser.model.ConfigNode;

public final class PropertiesParser implements LenientParser {

    private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

    @Override
    public ParserResult parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        List<String> joined = joinContinuations(lines);

        Map<String, ConfigNode> children = new LinkedHashMap<>();

        for (int i = 0; i < joined.size(); i++) {
            String raw = joined.get(i);
            String trimmed = raw.strip();

            if (trimmed.isEmpty() || isComment(trimmed)) {
                continue;
            }

            int sepIdx = findSeparator(trimmed);
            if (sepIdx < 0) {
                continue;
            }

            String key = trimmed.substring(0, sepIdx).strip();
            String value = decodeUnicode(trimmed.substring(sepIdx + 1).strip());

            if (key.isEmpty()) {
                continue;
            }

            putNested(children, key, value, i, raw);
        }

        return ParserResult.ok(new ConfigSection("", children, null, ""));
    }

    private static List<String> joinContinuations(List<String> lines) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            if (line.endsWith("\\") && !line.endsWith("\\\\")) {
                current.append(line, 0, line.length() - 1);
            } else {
                current.append(line);
                result.add(current.toString());
                current.setLength(0);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    static String decodeUnicode(String value) {
        Matcher m = UNICODE_ESCAPE.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int codePoint = Integer.parseInt(m.group(1), 16);
            m.appendReplacement(sb, String.valueOf((char) codePoint));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static void putNested(Map<String, ConfigNode> root, String key, String value, int line, String raw) {
        String[] parts = key.split("\\.");
        Map<String, ConfigNode> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            ConfigNode existing = current.get(part);
            if (existing instanceof ConfigSection section) {
                current = section.children();
            } else {
                ConfigSection newSection = new ConfigSection(part, new LinkedHashMap<>(), null, "");
                current.put(part, newSection);
                current = newSection.children();
            }
        }
        String leafKey = parts[parts.length - 1];
        current.put(leafKey, ValueInference.createValue(
                leafKey,
                value,
                new Provenance(line, null, raw, 1.0)
        ));
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
