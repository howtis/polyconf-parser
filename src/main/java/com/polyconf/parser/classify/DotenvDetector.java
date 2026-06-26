package com.polyconf.parser.classify;

import java.util.List;

public final class DotenvDetector extends FormatDetector {
    @Override
    public int score(List<Token> tokens) {
        int score = 0;
        boolean hasIniEquals = false;
        for (Token t : tokens) {
            if (t.kind() == TokenKind.DELIMITER && t.text().equals("=")
                    && !t.spaceBefore() && !t.spaceAfter()) {
                hasIniEquals = true;
            }
        }
        if (hasIniEquals) {
            score += 1;
        }
        if (!tokens.isEmpty()) {
            Token first = tokens.get(0);
            if (first.kind() == TokenKind.WORD && first.isUppercaseStart()
                    && hasEqualsAfter(tokens, 0)) {
                score += 2;
            }
            if (first.kind() == TokenKind.WORD && first.text().equalsIgnoreCase("export")) {
                score += 4;
            }
            Token last = tokens.get(tokens.size() - 1);
            if (first.text().equals("[") && last.text().equals("]")) {
                score -= 3;
            }
        }
        return score;
    }

    private static boolean hasEqualsAfter(List<Token> tokens, int from) {
        for (int i = from + 1; i < tokens.size(); i++) {
            if (tokens.get(i).text().equals("=")) return true;
        }
        return false;
    }
}
