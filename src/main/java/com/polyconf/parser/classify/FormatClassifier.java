package com.polyconf.parser.classify;

import com.polyconf.parser.model.Format;

import java.util.List;
import java.util.regex.Pattern;

public final class FormatClassifier {

    private static final Pattern HINT_LINE = Pattern.compile(
            "^\\s*#\\s*@fmt:\\w+\\s*$", Pattern.CASE_INSENSITIVE);

    private FormatClassifier() {
    }

    public static Format classify(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }
        if (lines.isEmpty()) {
            return Format.UNKNOWN;
        }

        int[] scores = new int[Format.values().length];
        int structuralLines = 0;
        for (String line : lines) {
            String t = line.strip();
            if (t.isEmpty() || HINT_LINE.matcher(line).matches()) {
                continue;
            }
            if (scoreLine(t, scores)) {
                structuralLines++;
            }
        }

        int bestOrdinal = -1;
        int bestScore = 0;
        boolean tie = false;

        for (int i = 0; i < scores.length; i++) {
            if (Format.values()[i] == Format.UNKNOWN) {
                continue;
            }
            if (scores[i] > bestScore) {
                bestScore = scores[i];
                bestOrdinal = i;
                tie = false;
            } else if (scores[i] == bestScore && bestScore > 0) {
                tie = true;
            }
        }

        if (bestScore == 0 || tie) {
            return Format.UNKNOWN;
        }
        if (structuralLines == 0 && bestScore <= 3) {
            return Format.UNKNOWN;
        }
        return Format.values()[bestOrdinal];
    }

    private static boolean scoreLine(String t, int[] scores) {
        if (scoreJson(t, scores)) return true;
        if (scoreXml(t, scores)) return true;
        if (scoreYamlDocSeparator(t, scores)) return true;

        boolean tomlArray = t.startsWith("[[") && t.contains("]]");
        boolean section = t.startsWith("[") && t.contains("]") && !t.startsWith("[[");
        boolean hasEquals = t.contains("=");
        boolean hasColon = t.contains(":");
        boolean tomlStyleKeyValue = isTomlStyle(t);
        boolean iniStyleKeyValue = isIniStyle(t);
        boolean semicolonComment = t.startsWith(";");
        boolean backslashEnd = t.endsWith("\\");
        boolean exportPrefix = t.toLowerCase().startsWith("export ");
        boolean uppercaseKey = hasEquals && Character.isUpperCase(firstNonWhitespace(t));
        boolean commaLine = t.contains(",") && !hasEquals && !hasColon && !t.startsWith("[") && !t.startsWith("<");

        boolean structural = tomlArray || section || commaLine || exportPrefix || uppercaseKey;

        if (tomlArray) {
            scores[Format.TOML.ordinal()] += 5;
        }
        if (section) {
            scores[Format.TOML.ordinal()] += 2;
            scores[Format.INI.ordinal()] += 2;
        }

        if (tomlStyleKeyValue) {
            scores[Format.TOML.ordinal()] += 3;
        }

        if (iniStyleKeyValue) {
            scores[Format.INI.ordinal()] += 2;
            scores[Format.PROPERTIES.ordinal()] += 2;
            scores[Format.DOTENV.ordinal()] += 1;
            if (hasDotBeforeEquals(t)) {
                scores[Format.PROPERTIES.ordinal()] += 3;
                scores[Format.INI.ordinal()] -= 1;
            }
        }

        if (t.contains(":") && !hasEquals && !t.startsWith("[") && !t.startsWith("<") && !t.startsWith("{")) {
            boolean dottedKey = hasDotBeforeColon(t);
            if (dottedKey) {
                scores[Format.PROPERTIES.ordinal()] += 3;
                scores[Format.YAML.ordinal()] += 2;
            } else {
                scores[Format.YAML.ordinal()] += 3;
                scores[Format.PROPERTIES.ordinal()] += 1;
            }
        }

        if (uppercaseKey) {
            scores[Format.DOTENV.ordinal()] += 2;
        }

        if (semicolonComment) {
            scores[Format.INI.ordinal()] += 1;
        }

        if (backslashEnd) {
            scores[Format.PROPERTIES.ordinal()] += 3;
        }

        if (exportPrefix) {
            scores[Format.DOTENV.ordinal()] += 4;
        }

        if (commaLine) {
            scores[Format.CSV.ordinal()] += 3;
        }

        return structural;
    }

    private static boolean isTomlStyle(String t) {
        int eq = t.indexOf('=');
        if (eq < 0) return false;
        boolean leftSpace = eq > 0 && t.charAt(eq - 1) == ' ';
        boolean rightSpace = eq < t.length() - 1 && t.charAt(eq + 1) == ' ';
        return leftSpace && rightSpace;
    }

    private static boolean isIniStyle(String t) {
        int eq = t.indexOf('=');
        if (eq < 0) return false;
        boolean leftSpace = eq > 0 && t.charAt(eq - 1) == ' ';
        boolean rightSpace = eq < t.length() - 1 && t.charAt(eq + 1) == ' ';
        return !leftSpace && !rightSpace;
    }

    private static boolean scoreJson(String t, int[] scores) {
        if (t.startsWith("{")) {
            scores[Format.JSON.ordinal()] += 5;
            return true;
        }
        if (t.startsWith("[")) {
            if (t.contains(":") || t.contains("\"") || t.equals("[")) {
                scores[Format.JSON.ordinal()] += 5;
                return true;
            }
        }
        if (t.startsWith("}") || t.startsWith("]")) {
            scores[Format.JSON.ordinal()] += 3;
            return true;
        }
        return false;
    }

    private static boolean scoreXml(String t, int[] scores) {
        if ((t.startsWith("<") && !t.startsWith("<-") && t.contains(">"))
                || t.startsWith("<?") || t.endsWith("?>")) {
            scores[Format.XML.ordinal()] += 5;
            return true;
        }
        return false;
    }

    private static boolean scoreYamlDocSeparator(String t, int[] scores) {
        if (t.equals("---") || t.equals("...")) {
            scores[Format.YAML.ordinal()] += 5;
            return true;
        }
        return false;
    }

    private static boolean hasDotBeforeEquals(String t) {
        int eq = t.indexOf('=');
        return eq > 0 && t.lastIndexOf('.', eq) >= 0;
    }

    private static boolean hasDotBeforeColon(String t) {
        int col = t.indexOf(':');
        return col > 0 && t.lastIndexOf('.', col) >= 0;
    }

    private static char firstNonWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }
        return '\0';
    }
}
