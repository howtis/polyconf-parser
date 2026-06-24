package com.polyconf.parser.classify;

public final class TomlDetector extends FormatDetector {
    @Override
    public int score(String t) {
        int score = 0;
        if (t.startsWith("[[") && t.contains("]]")) {
            score += 5;
        }
        if (t.startsWith("[") && t.contains("]") && !t.startsWith("[[")) {
            score += 2;
        }
        if (isTomlStyle(t)) {
            score += 3;
        }
        return score;
    }
}
