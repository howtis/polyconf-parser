package com.polyconf.parser.classify;

import java.util.List;

public final class XmlDetector extends FormatDetector {
    @Override
    public int score(List<Token> tokens) {
        if (tokens.isEmpty()) return 0;
        Token first = tokens.get(0);
        if (first.kind() == TokenKind.DELIMITER && first.text().equals("<")) {
            for (int i = 1; i < tokens.size(); i++) {
                if (tokens.get(i).text().equals(">")) {
                    return 5;
                }
            }
        }
        if (first.kind() == TokenKind.DELIMITER && first.text().equals("<?")) {
            return 5;
        }
        Token last = tokens.get(tokens.size() - 1);
        if (last.kind() == TokenKind.DELIMITER && last.text().equals("?>")) {
            return 5;
        }
        return 0;
    }
}
