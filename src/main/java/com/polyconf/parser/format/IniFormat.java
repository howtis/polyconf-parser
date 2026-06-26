package com.polyconf.parser.format;

import com.polyconf.parser.classify.FormatDetector;
import com.polyconf.parser.classify.Token;
import com.polyconf.parser.classify.TokenKind;
import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.parse.LenientParser;
import com.polyconf.parser.parse.ValueInference;
import org.ini4j.Ini;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IniFormat {

    private IniFormat() {}

    public static final class Detector extends FormatDetector {
        @Override
        public int score(List<Token> tokens) {
            int score = 0;
            if (tokens.size() >= 2) {
                Token first = tokens.get(0);
                if (first.kind() == TokenKind.DELIMITER && first.text().equals("[")) {
                    for (int i = 1; i < tokens.size(); i++) {
                        if (tokens.get(i).text().equals("]")) {
                            score += 2;
                            break;
                        }
                    }
                }
            }
            boolean hasIniEquals = false;
            for (Token t : tokens) {
                if (t.kind() == TokenKind.DELIMITER && t.text().equals("=")
                        && !t.spaceBefore() && !t.spaceAfter()) {
                    hasIniEquals = true;
                }
                if (t.text().equals(";")) {
                    score += 1;
                }
                if (t.text().equals("[[")) {
                    score -= 3;
                }
            }
            if (hasIniEquals) {
                score += 2;
                if (hasDottedKeyBeforeEquals(tokens)) {
                    score -= 1;
                }
            }

            // Negative scoring: patterns that clearly belong to other formats
            // INI uses ; for comments, not #
            if (!tokens.isEmpty() && tokens.get(0).text().equals("#")) {
                // # followed by content that is not a section header [section]
                if (tokens.size() < 2 || !tokens.get(1).text().equals("[")) {
                    score -= 2;
                }
            }
            // KDL-style escaped quotes
            for (Token t : tokens) {
                if (t.text().contains("\\\"")) {
                    score -= 3;
                    break;
                }
            }
            // // comments belong to KDL, not INI
            if (!tokens.isEmpty() && tokens.get(0).text().equals("//")) {
                score -= 3;
            }

            return score;
        }

        private static boolean hasDottedKeyBeforeEquals(List<Token> tokens) {
            for (Token t : tokens) {
                if (t.kind() == TokenKind.WORD && t.hasDot()) {
                    for (int i = tokens.indexOf(t) + 1; i < tokens.size(); i++) {
                        if (tokens.get(i).text().equals("=")) return true;
                    }
                }
                if (t.text().equals("=")) return false;
            }
            return false;
        }
    }

    public static final class Parser implements LenientParser {

        private static final String DUMMY_SECTION = "__polyconf_global__";

        @Override
        public boolean isPlausible(List<String> lines) {
            return lines.stream().anyMatch(l -> l.strip().startsWith("["));
        }

        @Override
        public ParserResult parse(List<String> lines) {
            if (lines == null) {
                throw new IllegalArgumentException("lines must not be null");
            }

            List<BlockDiagnostic> diagnostics = new ArrayList<>();

            try {
                String wrapped = wrapGlobalKeys(lines);
                Ini ini = new Ini();
                ini.load(new StringReader(wrapped));

                Map<String, ConfigNode> root = new LinkedHashMap<>();

                // Process global keys (wrapped in dummy section)
                Ini.Section globalSection = ini.get(DUMMY_SECTION);
                if (globalSection != null && !globalSection.isEmpty()) {
                    for (String key : globalSection.keySet()) {
                        String value = globalSection.get(key, String.class);
                        if (value != null) {
                            root.put(key, ValueInference.createValue(
                                    key, value,
                                    new Provenance(0, null, key + "=" + value, 1.0)
                            ));
                        }
                    }
                }

                // Process named sections
                for (String sectionName : ini.keySet()) {
                    if (sectionName.equals(DUMMY_SECTION) || sectionName.isEmpty()) {
                        continue;
                    }
                    Ini.Section section = ini.get(sectionName);
                    if (section == null || section.isEmpty()) {
                        continue;
                    }

                    String[] parts = sectionName.split("\\.");
                    Map<String, ConfigNode> current = root;
                    for (int i = 0; i < parts.length - 1; i++) {
                        String part = parts[i];
                        ConfigNode existing = current.get(part);
                        if (existing instanceof ConfigSection cs) {
                            current = cs.children();
                        } else {
                            ConfigSection newSection = new ConfigSection(
                                    part, new LinkedHashMap<>(), null, "");
                            current.put(part, newSection);
                            current = newSection.children();
                        }
                    }

                    String leafKey = parts[parts.length - 1];
                    Map<String, ConfigNode> leafChildren = new LinkedHashMap<>();
                    for (String key : section.keySet()) {
                        String value = section.get(key, String.class);
                        if (value != null) {
                            leafChildren.put(key, ValueInference.createValue(
                                    key, value,
                                    new Provenance(0, null, key + "=" + value, 1.0)
                            ));
                        }
                    }

                    ConfigNode existing = current.get(leafKey);
                    if (existing instanceof ConfigSection cs) {
                        cs.children().putAll(leafChildren);
                    } else {
                        current.put(leafKey, new ConfigSection(
                                leafKey, leafChildren, null, ""));
                    }
                }

                return new ParserResult(
                        new ConfigSection("", root, null, ""),
                        diagnostics
                );
            } catch (Exception e) {
                diagnostics.add(new BlockDiagnostic(
                        0, 0,
                        "INI parse error: " + e.getMessage(),
                        DiagnosticLevel.ERROR
                ));
                return new ParserResult(
                        new ConfigSection("", null, ""),
                        diagnostics
                );
            }
        }

        private static String wrapGlobalKeys(List<String> lines) {
            StringBuilder sb = new StringBuilder();
            boolean seenSection = false;
            boolean globalInserted = false;
            for (String line : lines) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("#")) {
                    sb.append(line).append('\n');
                    continue;
                }
                if (trimmed.startsWith("[")) {
                    seenSection = true;
                    sb.append(line).append('\n');
                } else if (!seenSection) {
                    if (!globalInserted) {
                        globalInserted = true;
                        sb.append('[').append(DUMMY_SECTION).append("]\n");
                    }
                    sb.append(line).append('\n');
                } else {
                    sb.append(line).append('\n');
                }
            }
            // Remove trailing newline to avoid extra blank line
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();
        }
    }
}
