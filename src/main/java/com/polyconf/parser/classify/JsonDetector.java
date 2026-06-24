package com.polyconf.parser.classify;

public final class JsonDetector extends FormatDetector {
    @Override
    public int score(String t) {
        int score = 0;
        if (t.startsWith("{")) {
            score += 5;
        } else if (t.startsWith("[")) {
            if (t.contains(":") || t.contains("\"") || t.equals("[")) {
                score += 5;
            }
        }
        if (t.startsWith("}") || t.startsWith("]")) {
            score += 3;
        }
        return score;
    }
}
