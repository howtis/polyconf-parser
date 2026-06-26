package com.polyconf.parser.format;

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
import com.polyconf.parser.parse.ValueInference;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Primitive;
import de.marhali.json5.exception.Json5Exception;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json5Format {

    private Json5Format() {}

    public static final class Detector extends FormatDetector {
        @Override
        public int score(List<Token> tokens) {
            int score = 0;
            if (tokens.isEmpty()) return score;

            for (Token t : tokens) {
                String text = t.text();

                if (text.startsWith("//")) score += 3;
                if (text.contains("/*") || text.contains("*/")) score += 2;
                if (text.endsWith(",") && text.length() > 1) score += 3;
                if (text.contains("'") && !t.isQuoted()) score += 2;
            }

            // Unquoted key pattern: WORD followed by colon
            for (int i = 0; i < tokens.size() - 1; i++) {
                Token current = tokens.get(i);
                Token next = tokens.get(i + 1);
                if (next.text().equals(":") && current.kind() == TokenKind.WORD && !current.isQuoted()) {
                    score += 2;
                }
            }

            return score;
        }
    }

    public static final class Parser implements LenientParser {

        private static final Json5 JSON5 = new Json5();

        @Override
        public ParserResult parse(List<String> lines) {
            if (lines == null) {
                throw new IllegalArgumentException("lines must not be null");
            }

            String text = String.join("\n", lines);
            if (text.isBlank()) {
                return ParserResult.ok(new ConfigSection("", null, ""));
            }

            Json5Element root = parsePartial(text);
            if (root == null) {
                return new ParserResult(
                        new ConfigSection("", null, ""),
                        List.of(new BlockDiagnostic(0, lines.size() - 1,
                                "JSON5 parse error: Could not parse input",
                                DiagnosticLevel.ERROR))
                );
            }

            Map<String, ConfigNode> children = new LinkedHashMap<>();
            if (root.isJson5Object()) {
                Json5Object obj = root.getAsJson5Object();
                for (Map.Entry<String, Json5Element> entry : obj.entrySet()) {
                    children.put(entry.getKey(), convertElement(entry.getKey(), entry.getValue(), 0));
                }
            } else if (root.isJson5Array()) {
                Json5Array arr = root.getAsJson5Array();
                List<ConfigNode> items = new ArrayList<>();
                for (int i = 0; i < arr.size(); i++) {
                    items.add(convertElement(String.valueOf(i), arr.get(i), 0));
                }
                children.put("root", new ConfigList("root", items, null, ""));
            }

            return ParserResult.ok(new ConfigSection("", children, null, ""));
        }

        /**
         * Try to parse the text, retrying with balanced braces for partial input.
         * The segmenter may split JSON5 blocks on blank lines, so the input can be
         * missing closing braces/brackets. We detect this and append the missing
         * delimiters to produce a valid parseable document.
         */
        private Json5Element parsePartial(String text) {
            try {
                return JSON5.parse(text);
            } catch (Json5Exception e) {
                System.err.println("=== Json5Parser parse error ===");
                System.err.println("Message: " + e.getMessage());
                String balanced = balanceDelimiters(text);
                if (balanced != null) {
                    try {
                        return JSON5.parse(balanced);
                    } catch (Json5Exception ignored) {
                        // fall through to null return
                    }
                }
                return null;
            }
        }

        /**
         * Append missing closing braces/brackets to balance the input.
         */
        private String balanceDelimiters(String text) {
            StringBuilder sb = new StringBuilder(text);
            boolean inSingleQuote = false;
            boolean inDoubleQuote = false;
            int braceDepth = 0;
            int bracketDepth = 0;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if (c == '\\' && (inSingleQuote || inDoubleQuote) && i + 1 < text.length()) {
                    i++;
                    continue;
                }

                if (c == '\'' && !inDoubleQuote) {
                    inSingleQuote = !inSingleQuote;
                    continue;
                }
                if (c == '"' && !inSingleQuote) {
                    inDoubleQuote = !inDoubleQuote;
                    continue;
                }

                if (inSingleQuote || inDoubleQuote) {
                    continue;
                }

                if (c == '{') {
                    braceDepth++;
                } else if (c == '}') {
                    braceDepth--;
                } else if (c == '[') {
                    bracketDepth++;
                } else if (c == ']') {
                    bracketDepth--;
                }
            }

            if (braceDepth <= 0 && bracketDepth <= 0) {
                return null;
            }

            for (int i = 0; i < bracketDepth; i++) {
                sb.append(']');
            }
            for (int i = 0; i < braceDepth; i++) {
                sb.append('}');
            }

            return sb.toString();
        }

        private ConfigNode convertElement(String key, Json5Element element, int line) {
            if (element.isJson5Object()) {
                Json5Object obj = element.getAsJson5Object();
                Map<String, ConfigNode> children = new LinkedHashMap<>();
                for (Map.Entry<String, Json5Element> entry : obj.entrySet()) {
                    children.put(entry.getKey(), convertElement(entry.getKey(), entry.getValue(), line));
                }
                return new ConfigSection(key, children, null, "");
            }
            if (element.isJson5Array()) {
                Json5Array arr = element.getAsJson5Array();
                List<ConfigNode> items = new ArrayList<>();
                for (int i = 0; i < arr.size(); i++) {
                    items.add(convertElement(String.valueOf(i), arr.get(i), line));
                }
                return new ConfigList(key, items, null, "");
            }
            if (element.isJson5Null()) {
                return new ConfigValue(key, null, ValueType.NULL, null, "");
            }

            Json5Primitive primitive = element.getAsJson5Primitive();
            Object rawValue;

            if (primitive.isBoolean()) {
                rawValue = primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                rawValue = primitive.getAsNumber();
            } else {
                rawValue = primitive.getAsString();
            }

            return ValueInference.createValue(key, rawValue, null);
        }
    }
}
