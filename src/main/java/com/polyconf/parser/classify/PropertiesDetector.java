package com.polyconf.parser.classify;

public final class PropertiesDetector extends FormatDetector {
    @Override
    public int score(String t) {
        int score = 0;
        if (t.startsWith("[") && t.endsWith("]")) {
            score -= 2;
        }
        if (isIniStyle(t)) {
            score += 2;
            if (hasDotBeforeEquals(t)) {
                score += 3;
            }
        }
        if (t.contains(":") && !t.contains("=") && !t.startsWith("[")
                && !t.startsWith("<") && !t.startsWith("{")) {
            boolean dottedKey = hasDotBeforeColon(t);
            score += dottedKey ? 3 : 1;
        }
        if (t.endsWith("\\")) {
            score += 3;
        }
        return score;
    }
}
