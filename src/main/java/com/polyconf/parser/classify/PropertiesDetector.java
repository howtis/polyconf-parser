package com.polyconf.parser.classify;

import java.util.List;

public final class PropertiesDetector extends FormatDetector {
    @Override
    public int score(List<Token> tokens) {
        int score = 0;
        if (!tokens.isEmpty()) {
            Token first = tokens.get(0);
            Token last = tokens.get(tokens.size() - 1);
            if (first.text().equals("[") && last.text().equals("]")) {
                score -= 2;
            }
        }

        boolean hasIniEquals = false;
        boolean hasColon = false;
        boolean hasEquals = false;
        boolean firstIsDelim = !tokens.isEmpty()
                && tokens.get(0).kind() == TokenKind.DELIMITER
                && ("[".equals(tokens.get(0).text())
                    || "<".equals(tokens.get(0).text())
                    || "{".equals(tokens.get(0).text()));

        for (Token t : tokens) {
            if (t.kind() == TokenKind.DELIMITER && t.text().equals("=")
                    && !t.spaceBefore() && !t.spaceAfter()) {
                hasIniEquals = true;
                hasEquals = true;
            }
            if (t.text().equals("=")) hasEquals = true;
            if (t.text().equals(":")) hasColon = true;
        }

        if (hasIniEquals) {
            score += 2;
            if (hasDottedKeyBeforeEquals(tokens)) {
                score += 3;
            }
        }

        if (hasColon && !hasEquals && !firstIsDelim) {
            score += hasDottedKeyBeforeColon(tokens) ? 3 : 1;
        }

        if (!tokens.isEmpty()) {
            Token last = tokens.get(tokens.size() - 1);
            if (last.text().endsWith("\\")) {
                score += 3;
            }
        }
        return score;
    }

    private static boolean hasDottedKeyBeforeEquals(List<Token> tokens) {
        for (Token t : tokens) {
            if (t.text().equals("=")) return false;
            if (t.kind() == TokenKind.WORD && t.hasDot()) return true;
        }
        return false;
    }

    private static boolean hasDottedKeyBeforeColon(List<Token> tokens) {
        for (Token t : tokens) {
            if (t.text().equals(":")) return false;
            if (t.kind() == TokenKind.WORD && t.hasDot()) return true;
        }
        return false;
    }
}
