package com.polyconf.parser.classify;

import java.util.List;

public final class IniDetector extends FormatDetector {
    @Override
    public int score(List<Token> tokens) {
        int score = 0;
        if (tokens.size() >= 2) {
            Token first = tokens.get(0);
            if (first.kind() == TokenKind.DELIMITER && first.text().equals("[")) {
                for (int i = 1; i < tokens.size(); i++) {
                    if (tokens.get(i).text().equals("]")) {
                        score += 2;
                        break;
                    }
                }
            }
        }
        boolean hasIniEquals = false;
        for (Token t : tokens) {
            if (t.kind() == TokenKind.DELIMITER && t.text().equals("=")
                    && !t.spaceBefore() && !t.spaceAfter()) {
                hasIniEquals = true;
            }
            if (t.text().equals(";")) {
                score += 1;
            }
        }
        if (hasIniEquals) {
            score += 2;
            if (hasDottedKeyBeforeEquals(tokens)) {
                score -= 1;
            }
        }
        return score;
    }

    private static boolean hasDottedKeyBeforeEquals(List<Token> tokens) {
        for (Token t : tokens) {
            if (t.kind() == TokenKind.WORD && t.hasDot()) {
                for (int i = tokens.indexOf(t) + 1; i < tokens.size(); i++) {
                    if (tokens.get(i).text().equals("=")) return true;
                }
            }
            if (t.text().equals("=")) return false;
        }
        return false;
    }
}
