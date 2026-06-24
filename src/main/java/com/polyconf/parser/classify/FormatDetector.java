package com.polyconf.parser.classify;

public abstract class FormatDetector {
    public abstract int score(String strippedLine);

    protected static boolean isTomlStyle(String t) {
        int eq = t.indexOf('=');
        if (eq < 0) return false;
        boolean leftSpace = eq > 0 && t.charAt(eq - 1) == ' ';
        boolean rightSpace = eq < t.length() - 1 && t.charAt(eq + 1) == ' ';
        return leftSpace && rightSpace;
    }

    protected static boolean isIniStyle(String t) {
        int eq = t.indexOf('=');
        if (eq < 0) return false;
        boolean leftSpace = eq > 0 && t.charAt(eq - 1) == ' ';
        boolean rightSpace = eq < t.length() - 1 && t.charAt(eq + 1) == ' ';
        return !leftSpace && !rightSpace;
    }

    protected static boolean hasDotBeforeEquals(String t) {
        int eq = t.indexOf('=');
        return eq > 0 && t.lastIndexOf('.', eq) >= 0;
    }

    protected static boolean hasDotBeforeColon(String t) {
        int col = t.indexOf(':');
        return col > 0 && t.lastIndexOf('.', col) >= 0;
    }

    protected static char firstNonWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }
        return '\0';
    }
}
