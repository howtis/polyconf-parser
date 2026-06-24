package com.polyconf.parser.classify;

public record Token(String text, TokenKind kind, boolean spaceBefore, boolean spaceAfter) {

    public boolean hasDot() {
        return text.indexOf('.') >= 0;
    }

    public boolean isUppercaseStart() {
        return !text.isEmpty() && Character.isUpperCase(text.charAt(0));
    }

    public boolean isNumberLiteral() {
        if (text.isEmpty()) return false;
        int i = 0;
        if (text.charAt(0) == '-' || text.charAt(0) == '+') i++;
        if (i >= text.length()) return false;
        boolean hasDigit = false;
        boolean hasDot = false;
        boolean hasExp = false;
        for (; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (c == '.') {
                if (hasDot) return false;
                hasDot = true;
            } else if (c == 'e' || c == 'E') {
                if (hasExp) return false;
                hasExp = true;
                hasDot = true;
                if (i + 1 < text.length() && (text.charAt(i + 1) == '+' || text.charAt(i + 1) == '-')) i++;
            } else {
                return false;
            }
        }
        return hasDigit;
    }

    public boolean isQuoted() {
        return kind == TokenKind.QUOTED;
    }
}
