package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.ValueType;

import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlParser implements LenientParser {

    @Override
    public ConfigSection parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        String text = String.join("\n", lines);
        if (text.isBlank()) {
            return new ConfigSection("", null, "");
        }

        Map<String, ConfigNode> children = new LinkedHashMap<>();
        try {
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(text);

            if (loaded instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) loaded;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    children.put(entry.getKey(), convertValue(entry.getKey(), entry.getValue()));
                }
            }
        } catch (Exception e) {
            return new ConfigSection("", children, null, "");
        }

        return new ConfigSection("", children, null, "");
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
