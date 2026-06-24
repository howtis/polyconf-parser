package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.model.ValueType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CsvParser implements LenientParser {

    @Override
    public ConfigSection parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        List<String> header = null;
        List<ConfigNode> rows = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String trimmed = raw.strip();

            if (trimmed.isEmpty()) {
                continue;
            }

            List<String> cells = splitCsv(trimmed);

            if (header == null) {
                header = cells;
            } else {
                Map<String, ConfigNode> rowData = new LinkedHashMap<>();
                for (int j = 0; j < cells.size() && j < header.size(); j++) {
                    rowData.put(header.get(j), new ConfigValue(
                            header.get(j),
                            cells.get(j),
                            ValueType.STRING,
                            new Provenance(i, null, raw, 1.0),
                            ""
                    ));
                }
                rows.add(new ConfigSection("row" + i, rowData, null, ""));
            }
        }

        if (header == null) {
            return new ConfigSection("", null, "");
        }

        return new ConfigSection("", Map.of("rows", new ConfigList("rows", rows, null, "")), null, "");
    }

    static List<String> splitCsv(String line) {
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
                } else if (c == ',') {
                    cells.add(current.toString().strip());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        cells.add(current.toString().strip());

        return cells;
    }
}
