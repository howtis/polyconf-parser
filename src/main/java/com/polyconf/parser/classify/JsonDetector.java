package com.polyconf.parser.classify;

public final class JsonDetector extends FormatDetector {
    @Override
    public int score(String t) {
        int score = 0;
        if (t.startsWith("{")) {
            score += 5;
        } else if (t.startsWith("[")) {
            if (t.contains(":") || t.contains("\"") || t.equals("[")) {
                score += 5;
            }
        }
        if (t.startsWith("}") || t.startsWith("]")) {
            score += 3;
        }
        if (t.startsWith("\"") && t.contains("\":")) {
            score += 3;
        }
        if (t.equals("true") || t.equals("false") || t.equals("null")) {
            score += 2;
        }
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
            score += 2;
        }
        if (isNumberLiteral(t)) {
            score += 1;
        }
        return score;
    }

    private static boolean isNumberLiteral(String t) {
        if (t.isEmpty()) return false;
        int i = 0;
        if (t.charAt(0) == '-' || t.charAt(0) == '+') i++;
        if (i >= t.length()) return false;
        boolean hasDigit = false;
        boolean hasDot = false;
        boolean hasExp = false;
        for (; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (c == '.') {
                if (hasDot) return false;
                hasDot = true;
            } else if (c == 'e' || c == 'E') {
                if (hasExp) return false;
                hasExp = true;
                hasDot = true;
                if (i + 1 < t.length() && (t.charAt(i + 1) == '+' || t.charAt(i + 1) == '-')) i++;
            } else {
                return false;
            }
        }
        return hasDigit;
    }
}
