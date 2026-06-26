package com.polyconf.parser.classify;

import java.util.List;

public final class HoconDetector extends FormatDetector {
    @Override
    public int score(List<Token> tokens) {
        int score = 0;
        if (tokens.isEmpty()) return score;

        for (Token t : tokens) {
            String text = t.text();

            if (text.contains("${")) score += 4;
            if (text.equals("include")) score += 3;
            if (text.contains("\"\"\"")) score += 3;
            if (text.equals("+=")) score += 3;
        }

        Token first = tokens.get(0);
        if (first.text().equals("{")) {
            score += 2;
        }
        if (first.text().equals("}")) {
            score += 2;
        }

        // key { block pattern (WORD followed by {)
        if (tokens.size() >= 2 && tokens.get(tokens.size() - 1).text().equals("{")) {
            Token beforeBrace = tokens.get(tokens.size() - 2);
            if (beforeBrace.kind() == TokenKind.WORD && !beforeBrace.isQuoted()) {
                score += 3;
            }
        }

        return score;
    }

    @Override
    public int signaturePriority() {
        return 90;
    }

    @Override
    public boolean hasSignature(List<String> lines) {
        boolean hasHoconSignal = false;
        boolean hasHoconEq = false;
        boolean hasDoubleSlashComment = false;
        boolean hasBlockBrace = false;

        for (String line : lines) {
            String stripped = line.strip();
            if (stripped.isEmpty()) continue;

            if (!hasHoconSignal) {
                if (stripped.contains("${?")
                        || stripped.contains("+=")
                        || (stripped.startsWith("include")
                            && (stripped.contains("\"") || stripped.contains("'")))) {
                    hasHoconSignal = true;
                }
            }
            if (stripped.contains(" = ")) {
                hasHoconEq = true;
            }
            if (stripped.startsWith("//")) {
                hasDoubleSlashComment = true;
            }
            if (stripped.endsWith("{")) {
                hasBlockBrace = true;
            }
        }

        // " = " with `//` or block-opening `{` indicates HOCON assignment syntax, not KDL
        if (!hasHoconSignal && hasHoconEq && (hasDoubleSlashComment || hasBlockBrace)) {
            hasHoconSignal = true;
        }

        return hasHoconSignal;
    }
}
