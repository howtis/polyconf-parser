package com.polyconf.parser.segment;

public record Segment(int startLine, int endLine) {
    public Segment {
        if (startLine < 0) {
            throw new IllegalArgumentException("startLine must be non-negative");
        }
        if (endLine < startLine) {
            throw new IllegalArgumentException("endLine must be >= startLine");
        }
    }
}
