package com.polyconf.parser.format;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.polyconf.parser.classify.FormatDetector;
import com.polyconf.parser.classify.Token;
import com.polyconf.parser.classify.TokenKind;
import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.ValueType;
import com.polyconf.parser.parse.LenientParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonFormat {

    private JsonFormat() {}

    public static final class Detector extends FormatDetector {
        @Override
        public int score(List<Token> tokens) {
            int score = 0;
            if (tokens.isEmpty()) return score;

            Token first = tokens.get(0);

            if (first.text().equals("{")) {
                score += 5;
            } else if (first.text().equals("[")) {
                boolean hasColonOrQuote = false;
                for (Token t : tokens) {
                    if (t.text().equals(":") || t.isQuoted()) {
                        hasColonOrQuote = true;
                        break;
                    }
                }
                if (hasColonOrQuote || tokens.size() == 1) {
                    score += 5;
                }
            }

            if (first.text().equals("}") || first.text().equals("]")) {
                score += 3;
            }

            if (first.isQuoted()) {
                boolean hasColonAfter = false;
                for (int i = 1; i < tokens.size(); i++) {
                    if (tokens.get(i).text().equals(":")) {
                        hasColonAfter = true;
                        break;
                    }
                }
                if (hasColonAfter) {
                    score += 3;
                }
            }

            if (tokens.size() == 1 && first.kind() == TokenKind.WORD) {
                String t = first.text();
                if (t.equals("true") || t.equals("false") || t.equals("null")) {
                    score += 2;
                }
            }

            if (tokens.size() == 1 && first.isQuoted()) {
                score += 2;
            }

            if (tokens.size() == 1 && first.kind() == TokenKind.WORD && first.isNumberLiteral()) {
                score += 1;
            }

            return score;
        }
    }

    public static final class Parser implements LenientParser {

        private static final Gson GSON = new GsonBuilder()
                .setLenient()
                .create();

        @Override
        public ParserResult parse(List<String> lines) {
            if (lines == null) {
                throw new IllegalArgumentException("lines must not be null");
            }

            String text = String.join("\n", lines);
            if (text.isBlank()) {
                return ParserResult.ok(new ConfigSection("", null, ""));
            }

            JsonElement root;
            try {
                root = parseLenient(text);
            } catch (Exception e) {
                return new ParserResult(
                        new ConfigSection("", null, ""),
                        List.of(new BlockDiagnostic(0, lines.size() - 1,
                                "JSON parse error: " + e.getMessage(),
                                DiagnosticLevel.ERROR))
                );
            }

            // ponytail: reject lenient-mode false positives (e.g. TOML [section] parsed as JSON array ["section"])
            if (root.isJsonArray()) {
                JsonArray arr = root.getAsJsonArray();
                if (arr.size() == 1 && arr.get(0).isJsonPrimitive() && arr.get(0).getAsJsonPrimitive().isString()
                        && !text.strip().startsWith("[\"")) {
                    return ParserResult.ok(new ConfigSection("", null, ""));
                }
            }

            Map<String, ConfigNode> children = new LinkedHashMap<>();
            if (root.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
                    children.put(entry.getKey(), convertElement(entry.getKey(), entry.getValue(), 0));
                }
            } else if (root.isJsonArray()) {
                List<ConfigNode> items = new ArrayList<>();
                JsonArray arr = root.getAsJsonArray();
                for (int i = 0; i < arr.size(); i++) {
                    items.add(convertElement(String.valueOf(i), arr.get(i), 0));
                }
                children.put("root", new ConfigList("root", items, null, ""));
            }

            return ParserResult.ok(new ConfigSection("", children, null, ""));
        }

        private JsonElement parseLenient(String text) {
            JsonReader reader = new JsonReader(new StringReader(text));
            reader.setLenient(true);
            return com.google.gson.JsonParser.parseReader(reader);
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
}
