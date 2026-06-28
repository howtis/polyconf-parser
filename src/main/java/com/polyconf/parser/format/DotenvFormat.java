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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DotenvFormat {

    private DotenvFormat() {}

    public static final class Detector extends FormatDetector {

        // Dotenv: export KEY=VALUE
        private static final Pattern DOTENV_EXPORT = Pattern.compile("^export\\s+[A-Za-z_]\\w*\\s*=.*");

        // Dotenv: ALL_CAPS key with quoted value, e.g. SECRET_KEY='...' or APP_NAME="..."
        private static final Pattern DOTENV_CAPS_QUOTED = Pattern.compile("^[A-Z_][A-Z0-9_]*\\s*=\\s*['\"]");

        // Dotenv: ALL_CAPS key followed by =, e.g. DB_HOST=localhost, NODE_ENV=test
        private static final Pattern DOTENV_CAPS_ASSIGN = Pattern.compile("^[A-Z_][A-Z0-9_]*\\s*=");

        private static final double MIN_RAW = -3.0;
        private static final double MAX_RAW = 7.0;

        @Override
        public double score(List<Token> tokens) {
            int raw = 0;
            boolean hasIniEquals = false;
            for (Token t : tokens) {
                if (t.kind() == TokenKind.DELIMITER && t.text().equals("=")
                        && !t.spaceBefore() && !t.spaceAfter()) {
                    hasIniEquals = true;
                }
            }
            if (hasIniEquals) {
                raw += 1;
            }
            if (!tokens.isEmpty()) {
                Token first = tokens.get(0);
                if (first.kind() == TokenKind.WORD && first.isUppercaseStart()
                        && hasEqualsAfter(tokens, 0)) {
                    raw += 2;
                }
                if (first.kind() == TokenKind.WORD && first.text().equalsIgnoreCase("export")) {
                    raw += 4;
                }
                Token last = tokens.get(tokens.size() - 1);
                if (first.text().equals("[") && last.text().equals("]")) {
                    raw -= 3;
                }
            }
            double span = MAX_RAW - MIN_RAW;
            double confidence = 0.5 + raw / span;
            return Math.max(0.0, Math.min(1.0, confidence));
        }

        private static boolean hasEqualsAfter(List<Token> tokens, int from) {
            for (int i = from + 1; i < tokens.size(); i++) {
                if (tokens.get(i).text().equals("=")) return true;
            }
            return false;
        }

        @Override
        public int signaturePriority() {
            return 80;
        }

        @Override
        public boolean hasSignature(List<String> lines) {
            boolean hasBraceBlock = false;
            boolean hasDotenvExport = false;
            boolean hasDotenvCapsQuoted = false;
            boolean hasDotenvCapsAssign = false;

            for (String line : lines) {
                String stripped = line.strip();
                if (stripped.isEmpty()) continue;

                // Brace blocks (excluding ${} variable interpolation) indicate
                // structured config, not DOTENV.
                if (!hasBraceBlock) {
                    int braceIdx = stripped.indexOf("{");
                    while (braceIdx >= 0) {
                        if (braceIdx == 0 || stripped.charAt(braceIdx - 1) != '$') {
                            hasBraceBlock = true;
                            break;
                        }
                        braceIdx = stripped.indexOf("{", braceIdx + 1);
                    }
                }

                if (!hasDotenvExport && DOTENV_EXPORT.matcher(stripped).matches()) {
                    hasDotenvExport = true;
                }
                if (!hasDotenvCapsQuoted && DOTENV_CAPS_QUOTED.matcher(stripped).find()) {
                    hasDotenvCapsQuoted = true;
                }
                if (!hasDotenvCapsAssign && DOTENV_CAPS_ASSIGN.matcher(stripped).find()) {
                    hasDotenvCapsAssign = true;
                }
            }

            if (hasBraceBlock) return false;
            return hasDotenvExport || hasDotenvCapsQuoted || hasDotenvCapsAssign;
        }
    }

    public static final class Parser implements LenientParser {

        private static final Pattern DOTENV_LINE = Pattern.compile(
                "^(?:export\\s+)?([A-Za-z_]\\w*)\\s*=\\s*(.*)$");
        private static final Pattern VAR_REF = Pattern.compile("\\$\\{([^}]+)}|\\$([A-Za-z_]\\w*)");

        @Override
        public ParserResult parse(List<String> lines) {
            if (lines == null) {
                throw new IllegalArgumentException("lines must not be null");
            }

            Map<String, String> rawValues = new LinkedHashMap<>();
            List<BlockDiagnostic> diagnostics = new ArrayList<>();
            int lineNum = 0;

            while (lineNum < lines.size()) {
                String raw = lines.get(lineNum);
                String trimmed = raw.strip();

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    lineNum++;
                    continue;
                }

                Matcher m = DOTENV_LINE.matcher(trimmed);
                if (!m.matches()) {
                    lineNum++;
                    continue;
                }

                String key = m.group(1);
                String value = m.group(2).strip();

                // Handle multiline quoted values
                if (!value.isEmpty() && (value.charAt(0) == '"' || value.charAt(0) == '\'')) {
                    char quote = value.charAt(0);
                    if (value.length() < 2 || value.charAt(value.length() - 1) != quote) {
                        StringBuilder multiline = new StringBuilder(value);
                        lineNum++;
                        while (lineNum < lines.size()) {
                            String nextLine = lines.get(lineNum);
                            multiline.append("\n").append(nextLine);
                            if (nextLine.strip().endsWith(String.valueOf(quote)) && !nextLine.strip().endsWith("\\" + quote)) {
                                value = multiline.toString();
                                break;
                            }
                            lineNum++;
                        }
                    }
                }

                rawValues.put(key, unquote(value));
                lineNum++;
            }

            // Variable expansion pass
            Map<String, ConfigNode> children = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : rawValues.entrySet()) {
                String resolved = resolve(entry.getKey(), entry.getValue(), rawValues, new ArrayList<>(), diagnostics);
                children.put(entry.getKey(), ValueInference.createValue(
                        entry.getKey(),
                        resolved,
                        new Provenance(0, null, "", 1.0)
                ));
            }

            return new ParserResult(new ConfigSection("", children, null, ""), diagnostics);
        }

        private String resolve(String key, String value, Map<String, String> rawValues,
                               List<String> visited, List<BlockDiagnostic> diagnostics) {
            if (visited.contains(key)) {
                diagnostics.add(new BlockDiagnostic(0, 0,
                        "Dotenv circular variable reference: " + String.join(" -> ", visited) + " -> " + key,
                        DiagnosticLevel.ERROR));
                return value;
            }
            visited.add(key);

            Matcher m = VAR_REF.matcher(value);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String refName = m.group(1) != null ? m.group(1) : m.group(2);
                String refValue = rawValues.get(refName);
                if (refValue != null) {
                    String resolvedRef = resolve(refName, refValue, rawValues, new ArrayList<>(visited), diagnostics);
                    m.appendReplacement(sb, Matcher.quoteReplacement(resolvedRef));
                } else {
                    diagnostics.add(new BlockDiagnostic(0, 0,
                            "Dotenv undefined variable: " + refName,
                            DiagnosticLevel.WARNING));
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
                }
            }
            m.appendTail(sb);
            return sb.toString();
        }

        private static String unquote(String value) {
            if (value.length() >= 2) {
                char first = value.charAt(0);
                char last = value.charAt(value.length() - 1);
                if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                    return value.substring(1, value.length() - 1);
                }
            }
            return value;
        }
    }
}
