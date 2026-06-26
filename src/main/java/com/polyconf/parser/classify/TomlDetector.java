package com.polyconf.parser.classify;

import java.util.List;

public final class TomlDetector extends FormatDetector {
    @Override
    public int score(List<Token> tokens) {
        int score = 0;
        if (tokens.size() >= 2) {
            Token first = tokens.get(0);
            if (first.kind() == TokenKind.DELIMITER && first.text().equals("[[")) {
                for (int i = 1; i < tokens.size(); i++) {
                    if (tokens.get(i).text().equals("]]")) {
                        score += 5;
                        break;
                    }
                }
            }
            if (first.kind() == TokenKind.DELIMITER && first.text().equals("[")) {
                for (int i = 1; i < tokens.size(); i++) {
                    if (tokens.get(i).text().equals("]")) {
                        score += 2;
                        break;
                    }
                }
            }
        }
        for (Token t : tokens) {
            if (t.kind() == TokenKind.DELIMITER && t.text().equals("=")
                    && t.spaceBefore() && t.spaceAfter()) {
                score += 3;
            }
            if (t.kind() == TokenKind.DELIMITER
                    && (t.text().equals("{") || t.text().equals("}"))) {
                score -= 5;
            }
        }
        return score;
    }
}
