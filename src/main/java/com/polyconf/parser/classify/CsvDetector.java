package com.polyconf.parser.classify;

public final class CsvDetector extends FormatDetector {
    @Override
    public int score(String t) {
        if (t.contains(",") && !t.contains("=") && !t.contains(":")
                && !t.startsWith("[") && !t.startsWith("<")) {
            return 3;
        }
        return 0;
    }
}
