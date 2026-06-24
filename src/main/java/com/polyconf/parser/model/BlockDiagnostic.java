package com.polyconf.parser.model;

public record BlockDiagnostic(
        int startLine,
        int endLine,
        String message,
        DiagnosticLevel level
) {
    public BlockDiagnostic {
        if (startLine < 0) {
            throw new IllegalArgumentException("startLine must be non-negative");
        }
        if (endLine < startLine) {
            throw new IllegalArgumentException("endLine must be >= startLine");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
