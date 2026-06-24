package com.polyconf.parser.parse;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.ValueType;

import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TomlParser implements LenientParser {

    @Override
    public ParserResult parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        String text = String.join("\n", lines);
        if (text.isBlank()) {
            return ParserResult.ok(new ConfigSection("", null, ""));
        }

        try {
            TomlParseResult result = Toml.parse(text);
            if (result.hasErrors()) {
                String errorMsg = result.errors().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining("; "));
                return new ParserResult(
                        new ConfigSection("", null, ""),
                        List.of(new BlockDiagnostic(0, lines.size() - 1,
                                "TOML parse error: " + errorMsg,
                                DiagnosticLevel.ERROR))
                );
            }
            Map<String, ConfigNode> children = new LinkedHashMap<>();

            for (String key : result.keySet()) {
                children.put(key, convertValue(key, result.get(key), result));
            }

            return ParserResult.ok(new ConfigSection("", children, null, ""));
        } catch (Exception e) {
            return new ParserResult(
                    new ConfigSection("", null, ""),
                    List.of(new BlockDiagnostic(0, lines.size() - 1,
                            "TOML parse error: " + e.getMessage(),
                            DiagnosticLevel.ERROR))
            );
        }
    }

    private ConfigNode convertValue(String key, Object value, TomlParseResult toml) {
        if (value == null) {
            return new ConfigValue(key, null, ValueType.NULL, null, "");
        }
        if (value instanceof TomlTable) {
            Map<String, ConfigNode> children = new LinkedHashMap<>();
            TomlTable table = (TomlTable) value;
            for (String entryKey : table.keySet()) {
                children.put(entryKey, convertValue(entryKey, table.get(entryKey), toml));
            }
            return new ConfigSection(key, children, null, "");
        }
        if (value instanceof TomlArray) {
            TomlArray array = (TomlArray) value;
            List<ConfigNode> items = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                items.add(convertValue(String.valueOf(i), array.get(i), toml));
            }
            return new ConfigList(key, items, null, "");
        }
        return convertScalar(key, value);
    }

    private ConfigValue convertScalar(String key, Object value) {
        if (value instanceof Boolean) {
            return new ConfigValue(key, value, ValueType.BOOLEAN, null, "");
        }
        if (value instanceof Long) {
            return new ConfigValue(key, value, ValueType.INTEGER, null, "");
        }
        if (value instanceof Double) {
            return new ConfigValue(key, value, ValueType.FLOAT, null, "");
        }
        if (value instanceof LocalDate) {
            return new ConfigValue(key, value, ValueType.DATE, null, "");
        }
        if (value instanceof OffsetDateTime || value instanceof LocalDateTime) {
            return new ConfigValue(key, value, ValueType.DATETIME, null, "");
        }
        return new ConfigValue(key, value.toString(), ValueType.STRING, null, "");
    }
}
