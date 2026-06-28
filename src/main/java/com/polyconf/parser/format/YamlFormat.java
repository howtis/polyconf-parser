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
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlFormat {

    private YamlFormat() {}

    public static final class Detector extends FormatDetector {
        private static final double MIN_RAW = -7.0;
        private static final double MAX_RAW = 5.0;

        @Override
        public double score(List<Token> tokens) {
            int raw = 0;
            if (tokens.size() == 1) {
                Token only = tokens.get(0);
                if (only.kind() == TokenKind.WORD
                        && (only.text().equals("---") || only.text().equals("..."))) {
                    raw += 5;
                }
            }

            if (!tokens.isEmpty()) {
                Token first = tokens.get(0);
                if (first.text().equals("{") || first.text().equals("[")) {
                    raw -= 2;
                }
                if (first.text().equals("}") || first.text().equals("]")) {
                    raw -= 2;
                }
            }

            boolean hasColon = false;
            boolean hasEquals = false;
            boolean firstIsBracket = !tokens.isEmpty()
                    && ("[".equals(tokens.get(0).text())
                        || "<".equals(tokens.get(0).text())
                        || "{".equals(tokens.get(0).text()));

            boolean isListItem = !tokens.isEmpty() && tokens.get(0).text().equals("-");

            for (Token t : tokens) {
                if (t.text().equals(":")) hasColon = true;
                if (t.text().equals("=")) {
                    hasEquals = true;
                    if (!isListItem) {
                        raw -= 5;
                    }
                }
            }

            if (hasColon && !hasEquals && !firstIsBracket) {
                boolean dottedKey = hasDottedKeyBeforeColon(tokens);
                int base = dottedKey ? 2 : 3;
                Token firstNonBlank = findFirstNonDelimiter(tokens);
                if (firstNonBlank != null && firstNonBlank.isQuoted()) {
                    base = Math.max(1, base - 2);
                }
                raw += base;
            }
            double span = MAX_RAW - MIN_RAW;
            double confidence = 0.5 + raw / span;
            return Math.max(0.0, Math.min(1.0, confidence));
        }

        private static Token findFirstNonDelimiter(List<Token> tokens) {
            for (Token t : tokens) {
                if (t.kind() != TokenKind.DELIMITER) return t;
            }
            return null;
        }

        private static boolean hasDottedKeyBeforeColon(List<Token> tokens) {
            for (Token t : tokens) {
                if (t.text().equals(":")) return false;
                if (t.kind() == TokenKind.WORD && t.hasDot()) return true;
            }
            return false;
        }
    }

    public static final class Parser implements LenientParser {

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
                } else {
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
            if (value instanceof Double || value instanceof Float) {
                return new ConfigValue(key, value, ValueType.FLOAT, null, "");
            }
            return ValueInference.createValue(key, value, null);
        }
    }
}
