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

        List<Segment> segments = new ArrayList<>();
        int blockStart = -1;

        for (int i = 0; i < lines.size(); i++) {
            boolean isBlank = lines.get(i).isBlank();
            boolean isHint = hintLines.contains(i);

            if (blockStart < 0) {
                if (!isBlank) {
                    blockStart = i;
                }
            } else {
                if (isBlank || isHint) {
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
}
