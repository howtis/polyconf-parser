package com.polyconf.parser.classify;

public final class YamlDetector extends FormatDetector {
    @Override
    public int score(String t) {
        int score = 0;
        if (t.equals("---") || t.equals("...")) {
            score += 5;
        }
        if (t.startsWith("{") || t.startsWith("[")) {
            score -= 2;
        }
        if (t.startsWith("}") || t.startsWith("]")) {
            score -= 2;
        }
        if (t.contains(":") && !t.contains("=") && !t.startsWith("[")
                && !t.startsWith("<") && !t.startsWith("{")) {
            boolean dottedKey = hasDotBeforeColon(t);
            int base = dottedKey ? 2 : 3;
            char first = firstNonWhitespace(t);
            if (first == '"' || first == '\'') {
                base = Math.max(1, base - 2);
            }
            score += base;
        }
        return score;
    }
}
