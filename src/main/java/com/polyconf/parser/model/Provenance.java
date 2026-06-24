package com.polyconf.parser.model;

public record Provenance(
        int sourceLine,
        Format sourceFormat,
        String rawText,
        double confidence
) {
    public Provenance {
        if (sourceLine < 0) {
            throw new IllegalArgumentException("sourceLine must be non-negative");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }
}
