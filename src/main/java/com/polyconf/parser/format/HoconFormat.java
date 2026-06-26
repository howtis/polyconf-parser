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
import com.polyconf.parser.parse.LenientParser;
import com.polyconf.parser.parse.ValueInference;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HoconFormat {

    private HoconFormat() {}

    public static final class Detector extends FormatDetector {
        @Override
        public int score(List<Token> tokens) {
            int score = 0;
            if (tokens.isEmpty()) return score;

            for (Token t : tokens) {
                String text = t.text();

                if (text.contains("${")) score += 4;
                if (text.equals("include")) score += 3;
                if (text.contains("\"\"\"")) score += 3;
                if (text.equals("+=")) score += 3;
            }

            Token first = tokens.get(0);
            if (first.text().equals("{")) {
                score += 2;
            }
            if (first.text().equals("}")) {
                score += 2;
            }

            // key { block pattern (WORD followed by {)
            if (tokens.size() >= 2 && tokens.get(tokens.size() - 1).text().equals("{")) {
                Token beforeBrace = tokens.get(tokens.size() - 2);
                if (beforeBrace.kind() == TokenKind.WORD && !beforeBrace.isQuoted()) {
                    score += 3;
                }
            }

            return score;
        }

        @Override
        public int signaturePriority() {
            return 90;
        }

        @Override
        public boolean hasSignature(List<String> lines) {
            boolean hasHoconSignal = false;
            boolean hasHoconEq = false;
            boolean hasDoubleSlashComment = false;
            boolean hasBlockBrace = false;

            for (String line : lines) {
                String stripped = line.strip();
                if (stripped.isEmpty()) continue;

                if (!hasHoconSignal) {
                    if (stripped.contains("${?")
                            || stripped.contains("+=")
                            || (stripped.startsWith("include")
                                && (stripped.contains("\"") || stripped.contains("'")))) {
                        hasHoconSignal = true;
                    }
                }
                if (stripped.contains(" = ")) {
                    hasHoconEq = true;
                }
                if (stripped.startsWith("//")) {
                    hasDoubleSlashComment = true;
                }
                if (stripped.endsWith("{")) {
                    hasBlockBrace = true;
                }
            }

            // " = " with `//` or block-opening `{` indicates HOCON assignment syntax, not KDL
            if (!hasHoconSignal && hasHoconEq && (hasDoubleSlashComment || hasBlockBrace)) {
                hasHoconSignal = true;
            }

            return hasHoconSignal;
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
                case NULL:
                case STRING:
                case BOOLEAN:
                case NUMBER:
                default:
                    return ValueInference.createValue(key, value.unwrapped(), null);
            }
        }
    }
}
