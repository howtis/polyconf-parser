package com.polyconf.parser.parse;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.model.ValueType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.polyconf.parser.model.ConfigNode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DotenvParser implements LenientParser {

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
            if (value.length() >= 1 && (value.charAt(0) == '"' || value.charAt(0) == '\'')) {
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
                String resolvedRef = resolve(key, refValue, rawValues, new ArrayList<>(visited), diagnostics);
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
