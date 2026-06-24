package com.polyconf.parser.model;

import java.util.List;
import java.util.Map;

public record ParseResult(
        ConfigSection root,
        List<BlockResult> blocks,
        List<BlockDiagnostic> diagnostics
) {
    public ParseResult {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        if (blocks == null) {
            throw new IllegalArgumentException("blocks must not be null");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("diagnostics must not be null");
        }
        blocks = List.copyOf(blocks);
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean hasWarnings() {
        return diagnostics.stream()
                .anyMatch(d -> d.level() == DiagnosticLevel.WARNING);
    }

    public boolean hasErrors() {
        return diagnostics.stream()
                .anyMatch(d -> d.level() == DiagnosticLevel.ERROR);
    }

    public Map<String, Object> flattened() {
        return new ConfigAccessor(root).asFlattenedMap();
    }
}
