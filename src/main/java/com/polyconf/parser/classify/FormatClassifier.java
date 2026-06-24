package com.polyconf.parser.classify;

import com.polyconf.parser.model.Format;

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
        for (Format format : Format.registeredFormats()) {
            FormatDetector detector = format.detector().orElse(null);
            if (detector == null) {
                continue;
            }
            int s = detector.score(t);
            if (s != 0) {
                scores.merge(format, s, Integer::sum);
            }
        }
    }

}
