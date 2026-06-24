package com.polyconf.parser.model;

public record BlockResult(
        int startLine,
        int endLine,
        Format detectedFormat,
        double confidence,
        boolean hinted,
        ConfigSection section
) {
    public BlockResult {
        if (startLine < 0) {
            throw new IllegalArgumentException("startLine must be non-negative");
        }
        if (endLine < startLine) {
            throw new IllegalArgumentException("endLine must be >= startLine");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
        if (detectedFormat == null) {
            throw new IllegalArgumentException("detectedFormat must not be null");
        }
        if (section == null) {
            throw new IllegalArgumentException("section must not be null");
        }
    }
}
