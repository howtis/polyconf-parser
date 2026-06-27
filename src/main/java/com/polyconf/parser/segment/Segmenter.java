package com.polyconf.parser.segment;

import com.polyconf.parser.hint.Hint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Segmenter {

    private Segmenter() {
    }

    public static List<Segment> segment(List<String> lines, List<Hint> hints) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }
        if (hints == null) {
            throw new IllegalArgumentException("hints must not be null");
        }

        Set<Integer> hintLines = hints.stream()
                .map(Hint::line)
                .collect(Collectors.toSet());

        List<Integer> depths = computeLineDepths(lines);

        List<Segment> segments = new ArrayList<>();
        int blockStart = -1;

        for (int i = 0; i < lines.size(); i++) {
            boolean isBlank = lines.get(i).isBlank();
            boolean isHint = hintLines.contains(i);
            boolean insideBlock = depths.get(i) > 0;

            if (blockStart < 0) {
                if (!isBlank) {
                    blockStart = i;
                }
            } else {
                if ((isBlank && !insideBlock) || isHint) {
                    segments.add(new Segment(blockStart, i - 1));
                    blockStart = -1;
                    if (isHint) {
                        blockStart = i;
                    }
                }
            }
        }

        if (blockStart >= 0) {
            segments.add(new Segment(blockStart, lines.size() - 1));
        }

        return List.copyOf(segments);
    }

    /**
     * Computes cumulative brace/bracket/XML-tag depth after each line,
     * respecting quote state to avoid counting braces inside strings.
     * Index-aligned with the input lines list.
     */
    public static List<Integer> computeLineDepths(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }
        List<Integer> depths = new ArrayList<>(lines.size());
        int braceDepth = 0;
        int bracketDepth = 0;
        int xmlDepth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (String line : lines) {
            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);
                if (c == '\\' && (inSingleQuote || inDoubleQuote) && j + 1 < line.length()) {
                    j++;
                    continue;
                }
                if (c == '\'' && !inDoubleQuote) {
                    inSingleQuote = !inSingleQuote;
                    continue;
                }
                if (c == '"' && !inSingleQuote) {
                    inDoubleQuote = !inDoubleQuote;
                    continue;
                }
                if (inSingleQuote || inDoubleQuote) {
                    continue;
                }
                if (c == '{') braceDepth++;
                else if (c == '}') braceDepth--;
                else if (c == '[') bracketDepth++;
                else if (c == ']') bracketDepth--;
                else if (c == '<' && j + 1 < line.length()) {
                    char next = line.charAt(j + 1);
                    if (next == '/') xmlDepth--;
                    else if (next != '?' && next != '!') xmlDepth++;
                }
            }
            depths.add(braceDepth + bracketDepth + xmlDepth);
        }
        return depths;
    }
}
