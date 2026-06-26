package com.polyconf.parser.parse;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.ValueType;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HoconParser implements LenientParser {

    @Override
    public ParserResult parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        String text = String.join("\n", lines);
        if (text.isBlank()) {
            return ParserResult.ok(new ConfigSection("", null, ""));
        }

        List<BlockDiagnostic> diags = new ArrayList<>();
        Config config;
        try {
            config = ConfigFactory.parseString(text).resolve();
        } catch (ConfigException e) {
            // Resolution failed (e.g., include files not found). Try without resolve.
            diags.add(new BlockDiagnostic(0, lines.size() - 1,
                    "HOCON resolve error (using unresolved config): " + e.getMessage(),
                    DiagnosticLevel.WARNING));
            try {
                config = ConfigFactory.parseString(text);
            } catch (ConfigException e2) {
                return new ParserResult(
                        new ConfigSection("", null, ""),
                        List.of(new BlockDiagnostic(0, lines.size() - 1,
                                "HOCON parse error: " + e2.getMessage(),
                                DiagnosticLevel.ERROR))
                );
            }
        }

        Map<String, ConfigNode> children = new LinkedHashMap<>();
        for (Map.Entry<String, com.typesafe.config.ConfigValue> entry : config.root().entrySet()) {
            children.put(entry.getKey(), convert(entry.getKey(), entry.getValue()));
        }

        return new ParserResult(new ConfigSection("", children, null, ""), diags);
    }

    private static ConfigNode convert(String key, com.typesafe.config.ConfigValue value) {
        switch (value.valueType()) {
            case OBJECT: {
                com.typesafe.config.ConfigObject obj = (com.typesafe.config.ConfigObject) value;
                Map<String, ConfigNode> children = new LinkedHashMap<>();
                for (Map.Entry<String, com.typesafe.config.ConfigValue> entry : obj.entrySet()) {
                    children.put(entry.getKey(), convert(entry.getKey(), entry.getValue()));
                }
                return new ConfigSection(key, children, null, "");
            }
            case LIST: {
                com.typesafe.config.ConfigList list = (com.typesafe.config.ConfigList) value;
                List<ConfigNode> items = new ArrayList<>();
                int idx = 0;
                for (com.typesafe.config.ConfigValue item : list) {
                    items.add(convert(String.valueOf(idx), item));
                    idx++;
                }
                return new ConfigList(key, items, null, "");
            }
            case STRING:
                return new ConfigValue(key, value.unwrapped(), ValueType.STRING, null, "");
            case NUMBER: {
                Number num = (Number) value.unwrapped();
                if (num.doubleValue() == num.longValue()) {
                    return new ConfigValue(key, num.longValue(), ValueType.INTEGER, null, "");
                }
                return new ConfigValue(key, num.doubleValue(), ValueType.FLOAT, null, "");
            }
            case BOOLEAN:
                return new ConfigValue(key, value.unwrapped(), ValueType.BOOLEAN, null, "");
            case NULL:
                return new ConfigValue(key, null, ValueType.NULL, null, "");
            default:
                return new ConfigValue(key, value.unwrapped(), ValueType.STRING, null, "");
        }
    }
}
