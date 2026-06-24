package com.polyconf.parser.hint;

import com.polyconf.parser.model.Format;

public record Hint(int line, Format format) {
    public Hint {
        if (line < 0) {
            throw new IllegalArgumentException("line must be non-negative");
        }
        if (format == null) {
            throw new IllegalArgumentException("format must not be null");
        }
    }
}
