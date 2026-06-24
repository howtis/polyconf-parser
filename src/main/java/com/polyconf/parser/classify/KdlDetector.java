package com.polyconf.parser.classify;

import java.util.List;

public final class KdlDetector extends FormatDetector {
    @Override
    public int score(List<Token> tokens) {
        int score = 0;
        if (tokens.isEmpty()) return score;

        // KDL identifiers can contain - and other special chars.
        // Check for slashdash comment: /- (characteristic KDL feature)
        for (Token t : tokens) {
            String text = t.text();
            if (t.kind() == TokenKind.WORD) {
                if (text.startsWith("/-")) {
                    score += 6;
                }
            }
        }

        // Check for #true, #false, #null -- KDL-specific boolean/null syntax
        for (Token t : tokens) {
            String text = t.text();
            if (t.kind() == TokenKind.WORD) {
                if ("#true".equals(text) || "#false".equals(text) || "#null".equals(text)) {
                    score += 4;
                }
            }
        }

        // Node with children: WORD { at end (strong KDL signal)
        int lastIdx = tokens.size() - 1;
        if (tokens.get(lastIdx).text().equals("{")) {
            for (int i = lastIdx - 1; i >= 0; i--) {
                Token prev = tokens.get(i);
                if (prev.kind() == TokenKind.WORD) {
                    score += 3;
                    // If there's a word argument between name and brace -> stronger
                    if (i > 0 && tokens.get(i - 1).kind() == TokenKind.WORD) {
                        score += 2;
                    }
                    break;
                }
            }
        }

        // KDL nodes with arguments: WORD followed by WORD or QUOTED (no =)
        // e.g. "host \"localhost\"" or "port 8080"
        for (int i = 1; i < tokens.size(); i++) {
            Token prev = tokens.get(i - 1);
            Token curr = tokens.get(i);
            if (prev.kind() == TokenKind.WORD
                    && (curr.kind() == TokenKind.WORD || curr.kind() == TokenKind.QUOTED)) {
                // Make sure there's no = between them
                boolean hasEquals = false;
                for (int j = 0; j < tokens.size(); j++) {
                    if (tokens.get(j).text().equals("=")) {
                        hasEquals = true;
                        break;
                    }
                }
                if (!hasEquals) {
                    score += 2;
                    break;
                }
            }
        }

        // Closing brace at start of line
        Token first = tokens.get(0);
        if (first.text().equals("}")) {
            score += 2;
        }

        // // comment (C-style comment, not used by other formats in this project)
        if (first.text().equals("//")) {
            score += 2;
        }

        return score;
    }
}
