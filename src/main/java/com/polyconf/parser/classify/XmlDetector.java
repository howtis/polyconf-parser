package com.polyconf.parser.classify;

public final class XmlDetector extends FormatDetector {
    @Override
    public int score(String t) {
        if ((t.startsWith("<") && !t.startsWith("<-") && t.contains(">"))
                || t.startsWith("<?") || t.endsWith("?>")) {
            return 5;
        }
        return 0;
    }
}
