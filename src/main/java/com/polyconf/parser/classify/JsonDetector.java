package com.polyconf.parser.classify;

import java.util.List;

public final class JsonDetector extends FormatDetector {
    @Override
    public int score(List<Token> tokens) {
        int score = 0;
        if (tokens.isEmpty()) return score;

        Token first = tokens.get(0);

        if (first.text().equals("{")) {
            score += 5;
        } else if (first.text().equals("[")) {
            boolean hasColonOrQuote = false;
            for (Token t : tokens) {
                if (t.text().equals(":") || t.isQuoted()) {
                    hasColonOrQuote = true;
                    break;
                }
            }
            if (hasColonOrQuote || tokens.size() == 1) {
                score += 5;
            }
        }

        if (first.text().equals("}") || first.text().equals("]")) {
            score += 3;
        }

        if (first.isQuoted()) {
            boolean hasColonAfter = false;
            for (int i = 1; i < tokens.size(); i++) {
                if (tokens.get(i).text().equals(":")) {
                    hasColonAfter = true;
                    break;
                }
            }
            if (hasColonAfter) {
                score += 3;
            }
        }

        if (tokens.size() == 1 && first.kind() == TokenKind.WORD) {
            String t = first.text();
            if (t.equals("true") || t.equals("false") || t.equals("null")) {
                score += 2;
            }
        }

        if (tokens.size() == 1 && first.isQuoted()) {
            score += 2;
        }

        if (tokens.size() == 1 && first.kind() == TokenKind.WORD && first.isNumberLiteral()) {
            score += 1;
        }

        return score;
    }
}
