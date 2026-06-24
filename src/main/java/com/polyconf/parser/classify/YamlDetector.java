package com.polyconf.parser.classify;

public final class YamlDetector extends FormatDetector {
    @Override
    public int score(String t) {
        int score = 0;
        if (t.equals("---") || t.equals("...")) {
            score += 5;
        }
        if (t.contains(":") && !t.contains("=") && !t.startsWith("[")
                && !t.startsWith("<") && !t.startsWith("{")) {
            boolean dottedKey = hasDotBeforeColon(t);
            score += dottedKey ? 2 : 3;
        }
        return score;
    }
}
