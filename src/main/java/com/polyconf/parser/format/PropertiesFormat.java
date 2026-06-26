package com.polyconf.parser.format;

import com.polyconf.parser.classify.FormatDetector;
import com.polyconf.parser.classify.Token;
import com.polyconf.parser.classify.TokenKind;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.parse.LenientParser;
import com.polyconf.parser.parse.ValueInference;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class PropertiesFormat {

    private PropertiesFormat() {}

    public static final class Detector extends FormatDetector {
        @Override
        public int score(List<Token> tokens) {
            int score = 0;
            if (!tokens.isEmpty()) {
                Token first = tokens.get(0);
                Token last = tokens.get(tokens.size() - 1);
                if (first.text().equals("[") && last.text().equals("]")) {
                    score -= 2;
                }
            }

            boolean hasIniEquals = false;
            boolean hasColon = false;
            boolean hasEquals = false;
            boolean firstIsDelim = !tokens.isEmpty()
                    && tokens.get(0).kind() == TokenKind.DELIMITER
                    && ("[".equals(tokens.get(0).text())
                        || "<".equals(tokens.get(0).text())
                        || "{".equals(tokens.get(0).text()));

            for (Token t : tokens) {
                if (t.kind() == TokenKind.DELIMITER && t.text().equals("=")
                        && !t.spaceBefore() && !t.spaceAfter()) {
                    hasIniEquals = true;
                    hasEquals = true;
                }
                if (t.text().equals("=")) hasEquals = true;
                if (t.text().equals(":")) hasColon = true;
            }

            if (hasIniEquals) {
                score += 2;
                if (hasDottedKeyBeforeEquals(tokens)) {
                    score += 3;
                }
            }

            if (hasColon && !hasEquals && !firstIsDelim) {
                score += hasDottedKeyBeforeColon(tokens) ? 3 : 1;
                score -= 3;
            }

            if (!tokens.isEmpty()) {
                Token last = tokens.get(tokens.size() - 1);
                if (last.text().endsWith("\\")) {
                    score += 3;
                }
            }

            // Negative scoring: patterns that clearly belong to KDL
            // // comments are KDL, not Properties
            if (!tokens.isEmpty() && tokens.get(0).text().equals("//")) {
                score -= 4;
            }
            // KDL booleans/null
            for (Token t : tokens) {
                String text = t.text();
                if ("#true".equals(text) || "#false".equals(text) || "#null".equals(text)) {
                    score -= 4;
                    break;
                }
            }
            // KDL-style escaped quotes in values
            for (Token t : tokens) {
                if (t.text().contains("\\\"")) {
                    score -= 3;
                    break;
                }
            }

            return score;
        }

        private static boolean hasDottedKeyBeforeEquals(List<Token> tokens) {
            for (Token t : tokens) {
                if (t.text().equals("=")) return false;
                if (t.kind() == TokenKind.WORD && t.hasDot()) return true;
            }
            return false;
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

            List<String> joined = joinContinuations(lines);

            Map<String, ConfigNode> children = new LinkedHashMap<>();
            for (int i = 0; i < joined.size(); i++) {
                String raw = joined.get(i);
                String trimmed = raw.strip();

                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    continue;
                }
                // Lines without a separator are skipped (java.util.Properties would treat them as key with empty value)
                if (!trimmed.contains("=") && !trimmed.contains(":")) {
                    continue;
                }

                // Use java.util.Properties for key=value parsing (unicode escapes, separators)
                Properties props = new Properties();
                try {
                    props.load(new StringReader(trimmed));
                } catch (IOException e) {
                    continue; // StringReader never throws IOException
                }
                if (props.isEmpty()) {
                    continue;
                }

                String key = props.stringPropertyNames().iterator().next();
                String value = props.getProperty(key);
                if (key.isEmpty()) {
                    continue;
                }
                putNested(children, key, value, i, raw);
            }

            return ParserResult.ok(new ConfigSection("", children, null, ""));
        }

        // keep our continuation joining -- java.util.Properties strips leading whitespace
        // on continuation lines, but our tests expect it preserved (e.g. "Hello \\\n  World" -> "Hello   World")
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
                    if (existing instanceof ConfigValue v) {
                        newSection.children().put("#self",
                                new ConfigValue("#self", v.rawValue(), v.type(), v.provenance(), ""));
                    }
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
    }
}
