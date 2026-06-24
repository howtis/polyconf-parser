package com.polyconf.parser.model;

import java.util.List;

public record ParserResult(
        ConfigSection section,
        List<BlockDiagnostic> diagnostics
) {
    public ParserResult {
        if (section == null) {
            throw new IllegalArgumentException("section must not be null");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("diagnostics must not be null");
        }
        diagnostics = List.copyOf(diagnostics);
    }

    public static ParserResult ok(ConfigSection section) {
        return new ParserResult(section, List.of());
    }
}
