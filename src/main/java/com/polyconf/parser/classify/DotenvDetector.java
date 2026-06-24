package com.polyconf.parser.classify;

public final class DotenvDetector extends FormatDetector {
    @Override
    public int score(String t) {
        int score = 0;
        if (isIniStyle(t)) {
            score += 1;
        }
        if (t.contains("=") && Character.isUpperCase(firstNonWhitespace(t))) {
            score += 2;
        }
        if (t.toLowerCase().startsWith("export ")) {
            score += 4;
        }
        return score;
    }
}
