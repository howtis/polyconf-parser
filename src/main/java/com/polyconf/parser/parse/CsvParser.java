package com.polyconf.parser.parse;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
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

public final class CsvParser implements LenientParser {

    private static final char[] DELIMITER_CANDIDATES = {',', '\t', ';', '|'};
    private final char delimiter;

    public CsvParser() {
        this('\0');
    }

    public CsvParser(char delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public ParserResult parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        char delim = delimiter != '\0' ? delimiter : detectDelimiter(lines);

        List<String> header = null;
        List<ConfigNode> rows = new ArrayList<>();
        List<BlockDiagnostic> diagnostics = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String trimmed = raw.strip();

            if (trimmed.isEmpty()) {
                continue;
            }

            boolean[] hadUnclosedQuote = new boolean[1];
            List<String> cells = splitCsv(trimmed, delim, hadUnclosedQuote);

            if (hadUnclosedQuote[0]) {
                diagnostics.add(new BlockDiagnostic(
                        i, i,
                        "CSV: unclosed quote in row " + i + " - data may be truncated",
                        DiagnosticLevel.WARNING
                ));
            }

            if (header == null) {
                header = cells;
            } else {
                Map<String, ConfigNode> rowData = new LinkedHashMap<>();
                for (int j = 0; j < cells.size() && j < header.size(); j++) {
                    rowData.put(header.get(j), createValue(
                            header.get(j),
                            cells.get(j),
                            new Provenance(i, null, raw, 1.0)
                    ));
                }
                rows.add(new ConfigSection("row" + i, rowData, null, ""));
            }
        }

        if (header == null) {
            return new ParserResult(new ConfigSection("", null, ""), diagnostics);
        }

        return new ParserResult(new ConfigSection("", Map.of("rows", new ConfigList("rows", rows, null, "")), null, ""), diagnostics);
    }

    private static ConfigValue createValue(String key, String raw, Provenance provenance) {
        return ValueInference.createValue(key, raw, provenance);
    }

    static char detectDelimiter(List<String> lines) {
        int[] counts = new int[DELIMITER_CANDIDATES.length];
        int linesChecked = 0;
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) continue;
            if (linesChecked >= 3) break;
            for (int i = 0; i < DELIMITER_CANDIDATES.length; i++) {
                for (int j = 0; j < trimmed.length(); j++) {
                    if (trimmed.charAt(j) == DELIMITER_CANDIDATES[i]) {
                        counts[i]++;
                    }
                }
            }
            linesChecked++;
        }
        int bestIdx = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > counts[bestIdx]) {
                bestIdx = i;
            }
        }
        return counts[bestIdx] > 0 ? DELIMITER_CANDIDATES[bestIdx] : ',';
    }

    static List<String> splitCsv(String line, char delim, boolean[] hadUnclosedQuote) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == delim) {
                    cells.add(current.toString().strip());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        cells.add(current.toString().strip());

        if (inQuotes) {
            hadUnclosedQuote[0] = true;
        }

        return cells;
    }
}
