package com.polyconf.parser.parse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.model.ValueType;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonParser implements LenientParser {

    private static final Gson GSON = new GsonBuilder()
            .setLenient()
            .create();

    @Override
    public ConfigSection parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        String text = String.join("\n", lines);
        if (text.isBlank()) {
            return new ConfigSection("", null, "");
        }

        JsonElement root;
        try {
            root = parseLenient(text);
        } catch (Exception e) {
            return new ConfigSection("", null, "");
        }

        Map<String, ConfigNode> children = new LinkedHashMap<>();
        if (root.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
                children.put(entry.getKey(), convertElement(entry.getKey(), entry.getValue(), 0));
            }
        }

        return new ConfigSection("", children, null, "");
    }

    private JsonElement parseLenient(String text) {
        try {
            JsonReader reader = new JsonReader(new StringReader(text));
            reader.setLenient(true);
            return com.google.gson.JsonParser.parseReader(reader);
        } catch (Exception e) {
            throw e;
        }
    }

    private ConfigNode convertElement(String key, JsonElement element, int line) {
        if (element.isJsonObject()) {
            Map<String, ConfigNode> children = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                children.put(entry.getKey(), convertElement(entry.getKey(), entry.getValue(), line));
            }
            return new ConfigSection(key, children, null, "");
        }
        if (element.isJsonArray()) {
            List<ConfigNode> items = new ArrayList<>();
            int idx = 0;
            for (JsonElement item : element.getAsJsonArray()) {
                items.add(convertElement(String.valueOf(idx), item, line));
                idx++;
            }
            return new ConfigList(key, items, null, "");
        }
        if (element.isJsonNull()) {
            return new ConfigValue(key, null, ValueType.NULL, null, "");
        }

        JsonPrimitive primitive = element.getAsJsonPrimitive();
        ValueType type = ValueType.STRING;
        Object rawValue;

        if (primitive.isBoolean()) {
            type = ValueType.BOOLEAN;
            rawValue = primitive.getAsBoolean();
        } else if (primitive.isNumber()) {
            Number num = primitive.getAsNumber();
            if (num.doubleValue() == num.longValue()) {
                type = ValueType.INTEGER;
                rawValue = num.longValue();
            } else {
                type = ValueType.FLOAT;
                rawValue = num.doubleValue();
            }
        } else {
            type = ValueType.STRING;
            rawValue = primitive.getAsString();
        }

        return new ConfigValue(key, rawValue, type, null, "");
    }
}
