package com.polyconf.parser.classify;

import com.polyconf.parser.model.Format;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FormatClassifier {

    private static final Pattern HINT_LINE = Pattern.compile(
            "^\\s*#\\s*@fmt:\\w+\\s*$", Pattern.CASE_INSENSITIVE);

    // Dotenv: $VARIABLE or ${VARIABLE} interpolation
    private static final Pattern DOTENV_INTERP = Pattern.compile("\\$\\{[A-Za-z_]\\w*}|\\$[A-Za-z_]\\w*");

    // Dotenv: export KEY=VALUE
    private static final Pattern DOTENV_EXPORT = Pattern.compile("^export\\s+[A-Za-z_]\\w*\\s*=.*");

    // Dotenv: URL with variable interpolation (postgres://${HOST}:${PORT}/...)
    private static final Pattern DOTENV_VAR_URL = Pattern.compile("[a-z]+://.*\\$\\{");

    // Dotenv: ALL_CAPS key with quoted value, e.g. SECRET_KEY='...' or APP_NAME="..."
    private static final Pattern DOTENV_CAPS_QUOTED = Pattern.compile("^[A-Z_][A-Z0-9_]*\\s*=\\s*['\"]");

    // Dotenv: ALL_CAPS key followed by =, e.g. DB_HOST=localhost, NODE_ENV=test
    private static final Pattern DOTENV_CAPS_ASSIGN = Pattern.compile("^[A-Z_][A-Z0-9_]*\\s*=");

    private FormatClassifier() {
    }

    public static Format classify(List<String> lines) {
        Format signatureHit = detectBySignature(lines);
        if (signatureHit != Format.UNKNOWN) {
            return signatureHit;
        }
        return classifyByScore(lines);
    }

    private static Format detectBySignature(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Format.UNKNOWN;
        }

        int xmlSignals = 0;
        boolean hasKdlSignal = false;
        boolean hasHoconSignal = false;
        boolean hasDoubleSlashComment = false;
        boolean hasBraceBlock = false;
        boolean hasBlockBrace = false;
        boolean hasHoconEq = false;
        boolean hasDotenvExport = false;
        boolean hasDotenvInterp = false;
        boolean hasDotenvCapsQuoted = false;
        boolean hasDotenvCapsAssign = false;

        Pattern xmlClosingTag = Pattern.compile("</[a-zA-Z]");
        Pattern xmlDeclaration = Pattern.compile("<\\?xml");

        for (String line : lines) {
            String stripped = line.strip();
            if (stripped.isEmpty()) {
                continue;
            }

            if (xmlDeclaration.matcher(stripped).find()) {
                xmlSignals++;
            }
            if (xmlClosingTag.matcher(stripped).find()) {
                xmlSignals++;
            }

            if (!hasKdlSignal) {
                if (stripped.contains("/-")
                        || stripped.contains("#true")
                        || stripped.contains("#false")
                        || stripped.contains("#null")
                        || stripped.contains("=\"")
                        || stripped.contains("='")) {
                    hasKdlSignal = true;
                }
            }
            if (stripped.startsWith("//")) {
                hasDoubleSlashComment = true;
            }
            // Detect brace blocks but exclude ${ variable interpolation (Dotenv/HOCON)
            int braceIdx = stripped.indexOf("{");
            while (braceIdx >= 0) {
                if (braceIdx == 0 || stripped.charAt(braceIdx - 1) != '$') {
                    hasBraceBlock = true;
                    if (stripped.endsWith("{")) {
                        hasBlockBrace = true;
                    }
                    break;
                }
                braceIdx = stripped.indexOf("{", braceIdx + 1);
            }

            if (!hasHoconEq && stripped.contains(" = ")) {
                hasHoconEq = true;
            }
            if (!hasHoconSignal) {
                if (stripped.contains("${?")
                        || stripped.contains("+=")
                        || (stripped.startsWith("include")
                            && (stripped.contains("\"") || stripped.contains("'")))) {
                    hasHoconSignal = true;
                }
            }

            if (!hasDotenvExport && DOTENV_EXPORT.matcher(stripped).matches()) {
                hasDotenvExport = true;
            }
            if (!hasDotenvInterp) {
                if (DOTENV_INTERP.matcher(stripped).find()
                        || DOTENV_VAR_URL.matcher(stripped).find()) {
                    hasDotenvInterp = true;
                }
            }
            if (!hasDotenvCapsQuoted && DOTENV_CAPS_QUOTED.matcher(stripped).find()) {
                hasDotenvCapsQuoted = true;
            }
            if (!hasDotenvCapsAssign) {
                if (DOTENV_CAPS_ASSIGN.matcher(stripped).find()
                        || DOTENV_EXPORT.matcher(stripped).matches()) {
                    hasDotenvCapsAssign = true;
                }
            }
        }

        if (xmlSignals >= 2) {
            return Format.XML;
        }
        // " = " with `//` or block-opening `{` indicates HOCON assignment syntax, not KDL
        if (!hasHoconSignal && hasHoconEq && (hasDoubleSlashComment || hasBlockBrace)) {
            hasHoconSignal = true;
        }
        if (hasHoconSignal) {
            return Format.HOCON;
        }
        // Dotenv: export keyword, ${VAR} interpolation, or ALL_CAPS key patterns
        if (!hasBraceBlock && !hasHoconSignal && xmlSignals < 2) {
            if (hasDotenvExport || hasDotenvInterp
                    || hasDotenvCapsQuoted || hasDotenvCapsAssign) {
                return Format.DOTENV;
            }
        }
        if ((hasKdlSignal || (hasDoubleSlashComment && hasBraceBlock))
                && !hasHoconSignal) {
            return Format.KDL;
        }
        return Format.UNKNOWN;
    }

    private static Format classifyByScore(List<String> lines) {
        Map<Format, Integer> scores = scoreMap(lines);

        Format bestFormat = null;
        int bestScore = 0;
        boolean tie = false;

        for (Map.Entry<Format, Integer> entry : scores.entrySet()) {
            Format format = entry.getKey();
            if (format == Format.UNKNOWN) {
                continue;
            }
            int s = entry.getValue();
            if (s > bestScore) {
                bestScore = s;
                bestFormat = format;
                tie = false;
            } else if (s == bestScore && bestScore > 0) {
                tie = true;
            }
        }

        if (bestScore == 0 || tie) {
            return Format.UNKNOWN;
        }
        return bestFormat;
    }

    public static Map<Format, Integer> scoreMap(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }
        if (lines.isEmpty()) {
            return Map.of();
        }

        Map<Format, Integer> scores = new LinkedHashMap<>();
        for (String line : lines) {
            String t = line.strip();
            if (t.isEmpty() || HINT_LINE.matcher(line).matches()) {
                continue;
            }
            scoreLine(t, scores);
        }
        return scores;
    }

    private static void scoreLine(String t, Map<Format, Integer> scores) {
        List<Token> tokens = LineTokenizer.tokenize(t);
        for (Format format : Format.registeredFormats()) {
            FormatDetector detector = format.detector().orElse(null);
            if (detector == null) {
                continue;
            }
            int s = detector.score(tokens);
            if (s != 0) {
                scores.merge(format, s, Integer::sum);
            }
        }
    }

}
