package com.polyconf.parser.classify;

public final class IniDetector extends FormatDetector {
    @Override
    public int score(String t) {
        int score = 0;
        if (t.startsWith("[") && t.contains("]") && !t.startsWith("[[")) {
            score += 2;
        }
        if (isIniStyle(t)) {
            score += 2;
            if (hasDotBeforeEquals(t)) {
                score -= 1;
            }
        }
        if (t.startsWith(";")) {
            score += 1;
        }
        return score;
    }
}
