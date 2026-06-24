package com.polyconf.parser.classify;

import java.util.ArrayList;
import java.util.List;

public final class LineTokenizer {

    private LineTokenizer() {
    }

    public static List<Token> tokenize(String line) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        boolean spaceBefore = false;

        while (i < line.length()) {
            char c = line.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                spaceBefore = true;
                continue;
            }

            if (c == '"' || c == '\'') {
                String quoted = readQuoted(line, i);
                int end = i + quoted.length();
                tokens.add(new Token(quoted, TokenKind.QUOTED, spaceBefore,
                        end < line.length() && Character.isWhitespace(line.charAt(end))));
                i = end;
                spaceBefore = false;
                continue;
            }

            if (isDelimiterChar(c)) {
                String delim = readDelimiter(line, i);
                int end = i + delim.length();
                tokens.add(new Token(delim, TokenKind.DELIMITER, spaceBefore,
                        end < line.length() && Character.isWhitespace(line.charAt(end))));
                i = end;
                spaceBefore = false;
                continue;
            }

            String word = readWord(line, i);
            int end = i + word.length();
            tokens.add(new Token(word, TokenKind.WORD, spaceBefore,
                    end < line.length() && Character.isWhitespace(line.charAt(end))));
            i = end;
            spaceBefore = false;
        }

        return tokens;
    }

    private static boolean isDelimiterChar(char c) {
        switch (c) {
            case '[': case ']': case '{': case '}':
            case '<': case '>': case '=': case ':':
            case ',': case ';': case '#':
                return true;
            default:
                return false;
        }
    }

    private static String readDelimiter(String line, int start) {
        char c = line.charAt(start);
        if (start + 1 < line.length()) {
            char next = line.charAt(start + 1);
            if (c == '[' && next == '[') return "[[";
            if (c == ']' && next == ']') return "]]";
            if (c == '<' && next == '?') return "<?";
            if (c == '?' && next == '>') return "?>";
        }
        return String.valueOf(c);
    }

    private static String readQuoted(String line, int start) {
        char quote = line.charAt(start);
        StringBuilder sb = new StringBuilder();
        sb.append(quote);
        for (int i = start + 1; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) {
                sb.append(c);
                sb.append(line.charAt(i + 1));
                i++;
            } else if (c == quote) {
                sb.append(c);
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String readWord(String line, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < line.length(); i++) {
            char c = line.charAt(i);
            if (Character.isWhitespace(c) || isDelimiterChar(c) || c == '"' || c == '\'') {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
