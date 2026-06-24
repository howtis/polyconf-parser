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
        if (t.startsWith("\"") && t.contains("\":")) {
            score += 3;
        }
        if (t.equals("true") || t.equals("false") || t.equals("null")) {
            score += 2;
        }
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
            score += 2;
        }
        return score;
    }
}
