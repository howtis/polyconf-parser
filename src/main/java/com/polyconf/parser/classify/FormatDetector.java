package com.polyconf.parser.classify;

import java.util.List;

public abstract class FormatDetector {
    /**
     * Returns confidence in [0.0, 1.0] that these tokens belong to this format.
     * 0.5 = neutral, 1.0 = strong match, 0.0 = strong contradiction.
     */
    public abstract double score(List<Token> tokens);

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
