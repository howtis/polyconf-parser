package com.polyconf.parser.classify;

import java.util.List;

public abstract class FormatDetector {
    public abstract int score(List<Token> tokens);

    /**
     * Priority for signature-based detection. Higher values are checked first.
     * Default 0 means this format does not participate in signature detection.
     */
    public int signaturePriority() {
        return 0;
    }

    /**
     * Checks whether the given lines contain this format's distinctive signature.
     * Only called for formats with {@code signaturePriority() > 0},
     * in descending priority order. The first match wins.
     */
    public boolean hasSignature(List<String> lines) {
        return false;
    }
}
