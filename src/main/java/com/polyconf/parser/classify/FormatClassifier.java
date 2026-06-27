package com.polyconf.parser.classify;

import com.polyconf.parser.model.Format;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class FormatClassifier {

    private static final Pattern HINT_LINE = Pattern.compile(
            "^\\s*#\\s*@fmt:\\w+\\s*$", Pattern.CASE_INSENSITIVE);

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

        return Format.registeredFormats().stream()
                .filter(f -> f.detector().isPresent())
                .sorted(Comparator.comparingInt(
                        (Format f) -> f.detector().get().signaturePriority()).reversed())
                .filter(f -> f.detector().get().signaturePriority() > 0)
                .filter(f -> f.detector().get().hasSignature(lines))
                .findFirst()
                .orElse(Format.UNKNOWN);
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

    public static List<Format> classifyPerLine(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }
        List<Format> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            String t = line.strip();
            if (t.isEmpty() || HINT_LINE.matcher(line).matches()) {
                result.add(Format.UNKNOWN);
                continue;
            }
            result.add(classifySingleLine(t));
        }
        return result;
    }

    private static Format classifySingleLine(String strippedLine) {
        // Bare braces are uniquely JSON.
        // Bare brackets ([, ]) are NOT treated as JSON because they also
        // appear as TOML array delimiters (e.g., "authors = [" followed by "]").
        if (strippedLine.equals("{") || strippedLine.equals("}")) {
            return Format.JSON;
        }

        List<Token> tokens = LineTokenizer.tokenize(strippedLine);
        Format bestFormat = Format.UNKNOWN;
        int bestScore = 0;
        boolean tie = false;

        for (Format format : Format.registeredFormats()) {
            FormatDetector detector = format.detector().orElse(null);
            if (detector == null) {
                continue;
            }
            int s = detector.score(tokens);
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
