package com.polyconf.parser.classify;

import java.util.List;

public final class CsvDetector extends FormatDetector {
    @Override
    public int score(List<Token> tokens) {
        int commas = 0;
        boolean hasEquals = false;
        boolean hasColon = false;
        for (Token t : tokens) {
            if (t.text().equals(",")) commas++;
            if (t.text().equals("=")) hasEquals = true;
            if (t.text().equals(":")) hasColon = true;
        }
        if (commas >= 2 && !hasEquals && !hasColon) {
            Token first = tokens.get(0);
            if (first.kind() == TokenKind.DELIMITER
                    && (first.text().equals("[") || first.text().equals("<"))) {
                return 0;
            }
            return 3;
        }
        return 0;
    }
}
