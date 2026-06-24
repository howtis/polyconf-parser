package com.polyconf.parser.classify;

import java.util.List;

public final class Json5Detector extends FormatDetector {
    @Override
    public int score(List<Token> tokens) {
        int score = 0;
        if (tokens.isEmpty()) return score;

        for (Token t : tokens) {
            String text = t.text();

            if (text.startsWith("//")) score += 3;
            if (text.contains("/*") || text.contains("*/")) score += 2;
            if (text.endsWith(",") && text.length() > 1) score += 3;
            if (text.contains("'") && !t.isQuoted()) score += 2;
        }

        // Unquoted key pattern: WORD followed by colon
        for (int i = 0; i < tokens.size() - 1; i++) {
            Token current = tokens.get(i);
            Token next = tokens.get(i + 1);
            if (next.text().equals(":") && current.kind() == TokenKind.WORD && !current.isQuoted()) {
                score += 2;
            }
        }

        return score;
    }
}
