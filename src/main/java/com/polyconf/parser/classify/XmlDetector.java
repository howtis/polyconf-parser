package com.polyconf.parser.classify;

import java.util.List;
import java.util.regex.Pattern;

public final class XmlDetector extends FormatDetector {

    private static final Pattern XML_CLOSING_TAG = Pattern.compile("</[a-zA-Z]");
    private static final Pattern XML_DECLARATION = Pattern.compile("<\\?xml");

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

    @Override
    public int signaturePriority() {
        return 100;
    }

    @Override
    public boolean hasSignature(List<String> lines) {
        int signals = 0;
        for (String line : lines) {
            String stripped = line.strip();
            if (stripped.isEmpty()) continue;
            if (XML_DECLARATION.matcher(stripped).find()) signals++;
            if (XML_CLOSING_TAG.matcher(stripped).find()) signals++;
            if (signals >= 2) return true;
        }
        return false;
    }
}
