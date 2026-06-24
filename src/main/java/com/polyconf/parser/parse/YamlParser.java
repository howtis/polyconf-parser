package com.polyconf.parser.parse;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.ValueType;

import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlParser implements LenientParser {

    @Override
    public ParserResult parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        String text = String.join("\n", lines);
        if (text.isBlank()) {
            return ParserResult.ok(new ConfigSection("", null, ""));
        }

        Map<String, ConfigNode> children = new LinkedHashMap<>();
        try {
            Yaml yaml = new Yaml();
            Iterable<Object> documents = yaml.loadAll(text);
            List<Object> docList = new ArrayList<>();
            for (Object doc : documents) {
                docList.add(doc);
            }

            // ponytail: reject scalar-only documents (e.g. "key=value" parsed as YAML scalars)
            boolean hasStructured = docList.stream().anyMatch(d -> d instanceof Map || d instanceof List);
            if (!hasStructured) {
                return ParserResult.ok(new ConfigSection("", null, ""));
            }

            if (docList.size() > 1) {
                for (int i = 0; i < docList.size(); i++) {
                    children.put(String.valueOf(i), convertDocument(String.valueOf(i), docList.get(i)));
                }
            } else if (docList.size() == 1) {
                Object loaded = docList.get(0);
                if (loaded instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) loaded;
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        children.put(entry.getKey(), convertValue(entry.getKey(), entry.getValue()));
                    }
                } else if (loaded instanceof List) {
                    children.put("root", convertList("root", (List<?>) loaded));
                }
            }
        } catch (StackOverflowError e) {
            return new ParserResult(
                    new ConfigSection("", children, null, ""),
                    List.of(new BlockDiagnostic(0, lines.size() - 1,
                            "YAML parse error: circular anchor reference detected",
                            DiagnosticLevel.ERROR))
            );
        } catch (Exception e) {
            return new ParserResult(
                    new ConfigSection("", children, null, ""),
                    List.of(new BlockDiagnostic(0, lines.size() - 1,
                            "YAML parse error: " + e.getMessage(),
                            DiagnosticLevel.ERROR))
            );
        }

        return ParserResult.ok(new ConfigSection("", children, null, ""));
    }

    private ConfigNode convertDocument(String key, Object doc) {
        if (doc instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) doc;
            Map<String, ConfigNode> children = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                children.put(entry.getKey(), convertValue(entry.getKey(), entry.getValue()));
            }
            return new ConfigSection(key, children, null, "");
        }
        if (doc instanceof List) {
            return convertList(key, (List<?>) doc);
        }
        return convertValue(key, doc);
    }

    private ConfigList convertList(String key, List<?> list) {
        List<ConfigNode> items = new ArrayList<>();
        int idx = 0;
        for (Object item : list) {
            items.add(convertValue(String.valueOf(idx), item));
            idx++;
        }
        return new ConfigList(key, items, null, "");
    }

    @SuppressWarnings("unchecked")
    private ConfigNode convertValue(String key, Object value) {
        if (value == null) {
            return new ConfigValue(key, null, ValueType.NULL, null, "");
        }
        if (value instanceof Map) {
            Map<String, ConfigNode> children = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                children.put(entry.getKey(), convertValue(entry.getKey(), entry.getValue()));
            }
            return new ConfigSection(key, children, null, "");
        }
        if (value instanceof List) {
            List<ConfigNode> items = new ArrayList<>();
            int idx = 0;
            for (Object item : (List<Object>) value) {
                items.add(convertValue(String.valueOf(idx), item));
                idx++;
            }
            return new ConfigList(key, items, null, "");
        }
        return convertScalar(key, value);
    }

    private ConfigValue convertScalar(String key, Object value) {
        if (value instanceof Boolean) {
            return new ConfigValue(key, value, ValueType.BOOLEAN, null, "");
        }
        if (value instanceof Integer || value instanceof Long) {
            return new ConfigValue(key, value, ValueType.INTEGER, null, "");
        }
        if (value instanceof Double || value instanceof Float) {
            return new ConfigValue(key, value, ValueType.FLOAT, null, "");
        }
        return new ConfigValue(key, value.toString(), ValueType.STRING, null, "");
    }
}
