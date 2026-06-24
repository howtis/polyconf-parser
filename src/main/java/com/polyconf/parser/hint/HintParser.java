package com.polyconf.parser.hint;

import com.polyconf.parser.model.Format;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HintParser {

    private static final Pattern HINT_PATTERN =
            Pattern.compile("^\\s*#\\s*@fmt:(\\w+)\\s*$", Pattern.CASE_INSENSITIVE);

    private HintParser() {
    }

    public static List<Hint> parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        List<Hint> hints = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = HINT_PATTERN.matcher(line);
            if (matcher.matches()) {
                String formatName = matcher.group(1);
                Format format = parseFormat(formatName);
                hints.add(new Hint(i, format));
            }
        }
        return List.copyOf(hints);
    }

    private static Format parseFormat(String name) {
        try {
            return Format.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Format.UNKNOWN;
        }
    }
}
