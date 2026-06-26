package com.polyconf.parser.classify;

import java.util.List;

public final class YamlDetector extends FormatDetector {
    @Override
    public int score(List<Token> tokens) {
        int score = 0;
        if (tokens.size() == 1) {
            Token only = tokens.get(0);
            if (only.kind() == TokenKind.WORD
                    && (only.text().equals("---") || only.text().equals("..."))) {
                score += 5;
            }
        }

        if (!tokens.isEmpty()) {
            Token first = tokens.get(0);
            if (first.text().equals("{") || first.text().equals("[")) {
                score -= 2;
            }
            if (first.text().equals("}") || first.text().equals("]")) {
                score -= 2;
            }
        }

        boolean hasColon = false;
        boolean hasEquals = false;
        boolean firstIsBracket = !tokens.isEmpty()
                && ("[".equals(tokens.get(0).text())
                    || "<".equals(tokens.get(0).text())
                    || "{".equals(tokens.get(0).text()));

        boolean isListItem = !tokens.isEmpty() && tokens.get(0).text().equals("-");

        for (Token t : tokens) {
            if (t.text().equals(":")) hasColon = true;
            if (t.text().equals("=")) {
                hasEquals = true;
                if (!isListItem) {
                    score -= 5;
                }
            }
        }

        if (hasColon && !hasEquals && !firstIsBracket) {
            boolean dottedKey = hasDottedKeyBeforeColon(tokens);
            int base = dottedKey ? 2 : 3;
            Token firstNonBlank = findFirstNonDelimiter(tokens);
            if (firstNonBlank != null && firstNonBlank.isQuoted()) {
                base = Math.max(1, base - 2);
            }
            score += base;
        }
        return score;
    }

    private static Token findFirstNonDelimiter(List<Token> tokens) {
        for (Token t : tokens) {
            if (t.kind() != TokenKind.DELIMITER) return t;
        }
        return null;
    }

    private static boolean hasDottedKeyBeforeColon(List<Token> tokens) {
        for (Token t : tokens) {
            if (t.text().equals(":")) return false;
            if (t.kind() == TokenKind.WORD && t.hasDot()) return true;
        }
        return false;
    }
}
