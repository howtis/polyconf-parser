package com.polyconf.parser.classify;

public final class CsvDetector extends FormatDetector {
    @Override
    public int score(String t) {
        int commas = 0;
        for (int i = 0; i < t.length(); i++) {
            if (t.charAt(i) == ',') commas++;
        }
        if (commas >= 2 && !t.contains("=") && !t.contains(":")
                && !t.startsWith("[") && !t.startsWith("<")) {
            return 3;
        }
        return 0;
    }
}
