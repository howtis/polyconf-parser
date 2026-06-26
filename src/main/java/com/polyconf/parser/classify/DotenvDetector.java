package com.polyconf.parser.classify;

import java.util.List;
import java.util.regex.Pattern;

public final class DotenvDetector extends FormatDetector {

    // Dotenv: export KEY=VALUE
    private static final Pattern DOTENV_EXPORT = Pattern.compile("^export\\s+[A-Za-z_]\\w*\\s*=.*");

    // Dotenv: ALL_CAPS key with quoted value, e.g. SECRET_KEY='...' or APP_NAME="..."
    private static final Pattern DOTENV_CAPS_QUOTED = Pattern.compile("^[A-Z_][A-Z0-9_]*\\s*=\\s*['\"]");

    // Dotenv: ALL_CAPS key followed by =, e.g. DB_HOST=localhost, NODE_ENV=test
    private static final Pattern DOTENV_CAPS_ASSIGN = Pattern.compile("^[A-Z_][A-Z0-9_]*\\s*=");

    @Override
    public int score(List<Token> tokens) {
        int score = 0;
        boolean hasIniEquals = false;
        for (Token t : tokens) {
            if (t.kind() == TokenKind.DELIMITER && t.text().equals("=")
                    && !t.spaceBefore() && !t.spaceAfter()) {
                hasIniEquals = true;
            }
        }
        if (hasIniEquals) {
            score += 1;
        }
        if (!tokens.isEmpty()) {
            Token first = tokens.get(0);
            if (first.kind() == TokenKind.WORD && first.isUppercaseStart()
                    && hasEqualsAfter(tokens, 0)) {
                score += 2;
            }
            if (first.kind() == TokenKind.WORD && first.text().equalsIgnoreCase("export")) {
                score += 4;
            }
            Token last = tokens.get(tokens.size() - 1);
            if (first.text().equals("[") && last.text().equals("]")) {
                score -= 3;
            }
        }
        return score;
    }

    private static boolean hasEqualsAfter(List<Token> tokens, int from) {
        for (int i = from + 1; i < tokens.size(); i++) {
            if (tokens.get(i).text().equals("=")) return true;
        }
        return false;
    }

    @Override
    public int signaturePriority() {
        return 80;
    }

    @Override
    public boolean hasSignature(List<String> lines) {
        boolean hasBraceBlock = false;
        boolean hasDotenvExport = false;
        boolean hasDotenvCapsQuoted = false;
        boolean hasDotenvCapsAssign = false;

        for (String line : lines) {
            String stripped = line.strip();
            if (stripped.isEmpty()) continue;

            // Brace blocks (excluding ${} variable interpolation) indicate
            // structured config, not DOTENV.
            if (!hasBraceBlock) {
                int braceIdx = stripped.indexOf("{");
                while (braceIdx >= 0) {
                    if (braceIdx == 0 || stripped.charAt(braceIdx - 1) != '$') {
                        hasBraceBlock = true;
                        break;
                    }
                    braceIdx = stripped.indexOf("{", braceIdx + 1);
                }
            }

            if (!hasDotenvExport && DOTENV_EXPORT.matcher(stripped).matches()) {
                hasDotenvExport = true;
            }
            if (!hasDotenvCapsQuoted && DOTENV_CAPS_QUOTED.matcher(stripped).find()) {
                hasDotenvCapsQuoted = true;
            }
            if (!hasDotenvCapsAssign && DOTENV_CAPS_ASSIGN.matcher(stripped).find()) {
                hasDotenvCapsAssign = true;
            }
        }

        if (hasBraceBlock) return false;
        return hasDotenvExport || hasDotenvCapsQuoted || hasDotenvCapsAssign;
    }
}
