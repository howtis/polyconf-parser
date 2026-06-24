package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.model.ValueType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KdlParser implements LenientParser {

    @Override
    public ParserResult parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        String joined = joinAndStripComments(lines);
        if (joined.isBlank()) {
            return ParserResult.ok(new ConfigSection("", new LinkedHashMap<>(), null, ""));
        }

        Map<String, ConfigNode> children = new LinkedHashMap<>();
        Pos pos = new Pos(joined, 0);
        parseNodes(pos, children, 0);
        return ParserResult.ok(new ConfigSection("", children, null, ""));
    }

    // ----- comment stripping -----

    private static String joinAndStripComments(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.stripLeading().startsWith("/-")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        String joined = sb.toString();

        // Strip block comments /* ... */ (supports nesting)
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < joined.length()) {
            if (i + 1 < joined.length() && joined.charAt(i) == '/' && joined.charAt(i + 1) == '*') {
                i += 2;
                int depth = 1;
                while (i < joined.length() && depth > 0) {
                    if (i + 1 < joined.length() && joined.charAt(i) == '*' && joined.charAt(i + 1) == '/') {
                        depth--;
                        i += 2;
                    } else if (i + 1 < joined.length() && joined.charAt(i) == '/' && joined.charAt(i + 1) == '*') {
                        depth++;
                        i += 2;
                    } else {
                        i++;
                    }
                }
                continue;
            }
            if (i + 1 < joined.length() && joined.charAt(i) == '/' && joined.charAt(i + 1) == '/') {
                while (i < joined.length() && joined.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            result.append(joined.charAt(i));
            i++;
        }
        return result.toString();
    }

    // ----- node parsing -----

    private static class Pos {
        final String text;
        int i;

        Pos(String text, int i) {
            this.text = text;
            this.i = i;
        }

        char peek() {
            return i < text.length() ? text.charAt(i) : '\0';
        }

        void advance() {
            i++;
        }

        void skipWs() {
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
        }

        boolean done() {
            return i >= text.length();
        }
    }

    private void parseNodes(Pos pos, Map<String, ConfigNode> parent, int depth) {
        while (!pos.done()) {
            pos.skipWs();
            if (pos.done()) break;

            char c = pos.peek();

            // Closing brace -> return to parent
            if (c == '}') {
                pos.advance();
                return;
            }

            // Semicolon terminates current node
            if (c == ';') {
                pos.advance();
                continue;
            }

            // Parse a node
            parseNode(pos, parent, depth);
        }
    }

    private void parseNode(Pos pos, Map<String, ConfigNode> parent, int depth) {
        pos.skipWs();
        if (pos.done()) return;

        // Node name (identifier or quoted string)
        String name = parseIdentifier(pos);
        if (name == null || name.isEmpty()) {
            skipRest(pos);
            return;
        }

        pos.skipWs();

        List<Object> args = new ArrayList<>();
        Map<String, ConfigNode> properties = new LinkedHashMap<>();
        Map<String, ConfigNode> children = new LinkedHashMap<>();

        boolean hasChildren = false;

        // Parse arguments and properties until newline, ;, {, or EOF
        while (!pos.done()) {
            char c = pos.peek();

            if (c == '\n' || c == ';' || c == '}') {
                break;
            }

            if (c == '{') {
                hasChildren = true;
                break;
            }

            if (c == '=') {
                // This is a property value (the key was already consumed)
                // Actually, properties in KDL are key=value on the same node line
                break;
            }

            pos.skipWs();
            if (pos.done()) break;

            c = pos.peek();
            if (c == '\n' || c == ';' || c == '}' || c == '{') break;

            // Look ahead: is this `key=value` or just a value?
            int savedPos = pos.i;
            String maybeKey = parseIdentifier(pos);
            pos.skipWs();
            if (!pos.done() && pos.peek() == '=' && maybeKey != null) {
                // This is a property: key=value
                pos.advance(); // consume =
                pos.skipWs();
                Object value = parseValue(pos);
                String key = unquote(maybeKey);
                properties.put(key, toConfigValue(key, value));
            } else if (maybeKey != null) {
                // This is an argument (positional value)
                pos.i = savedPos; // backtrack
                Object value = parseValue(pos);
                args.add(value);
            }
        }

        pos.skipWs();

        // Parse children block
        if (!pos.done() && pos.peek() == '{') {
            pos.advance(); // consume {
            parseNodes(pos, children, depth + 1);
            hasChildren = true;
        }

        // Consume trailing semicolon if present as node separator
        if (!hasChildren) {
            pos.skipWs();
            if (!pos.done() && pos.peek() == ';') {
                pos.advance();
            }
        }

        // Build ConfigSection for this node
        Map<String, ConfigNode> nodeContents = new LinkedHashMap<>();

        // Add positional arguments as numbered keys
        for (int i = 0; i < args.size(); i++) {
            nodeContents.put(String.valueOf(i), toConfigValue(String.valueOf(i), args.get(i)));
        }

        // Add properties
        nodeContents.putAll(properties);

        // Add children
        if (!children.isEmpty()) {
            ConfigSection childSection = new ConfigSection("", children, null, "");
            // Merge child nodes into node contents
            for (Map.Entry<String, ConfigNode> entry : children.entrySet()) {
                if (nodeContents.containsKey(entry.getKey())) {
                    // Duplicate child node name: store under indexed key
                    int idx = 0;
                    while (nodeContents.containsKey(entry.getKey() + "_" + idx)) {
                        idx++;
                    }
                    nodeContents.put(entry.getKey() + "_" + idx, entry.getValue());
                } else {
                    nodeContents.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Handle duplicate node names in parent
        if (parent.containsKey(name)) {
            int idx = 1;
            while (parent.containsKey(name + "_" + idx)) {
                idx++;
            }
            parent.put(name + "_" + idx, new ConfigSection(name, nodeContents, null, name));
        } else {
            parent.put(name, new ConfigSection(name, nodeContents, null, name));
        }
    }

    // ----- value parsing -----

    private Object parseValue(Pos pos) {
        pos.skipWs();
        if (pos.done()) return "";

        char c = pos.peek();

        // Quoted string
        if (c == '"') {
            return parseQuotedString(pos, '"');
        }

        // Raw string #"..."# or ##"..."## etc.
        if (c == '#') {
            // Check for #true, #false, #null
            String word = parseWord(pos);
            if ("#true".equals(word)) return Boolean.TRUE;
            if ("#false".equals(word)) return Boolean.FALSE;
            if ("#null".equals(word)) return null;
            // Raw string: count #s, then " must follow
            if (word.startsWith("#") && word.endsWith("\"")) {
                return parseRawStringValue(pos, word);
            }
            return word;
        }

        // Number or unquoted string
        String word = parseWord(pos);
        if (word.isEmpty()) return "";

        // Try number parsing
        try {
            if (word.startsWith("0x") || word.startsWith("0X")) {
                return Long.parseLong(word.substring(2), 16);
            }
            if (word.startsWith("0o") || word.startsWith("0O")) {
                return Long.parseLong(word.substring(2), 8);
            }
            if (word.startsWith("0b") || word.startsWith("0B")) {
                return Long.parseLong(word.substring(2), 2);
            }
            // Remove underscores
            String clean = word.replace("_", "");
            if (clean.contains(".") || clean.contains("e") || clean.contains("E")) {
                return Double.parseDouble(clean);
            }
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            // Not a number, treat as string
        }

        return word;
    }

    private String parseQuotedString(Pos pos, char quote) {
        StringBuilder sb = new StringBuilder();
        pos.advance(); // consume opening quote

        // Check for triple quote (multiline)
        boolean triple = false;
        if (!pos.done() && pos.peek() == quote && pos.i + 1 < pos.text.length() && pos.text.charAt(pos.i + 1) == quote) {
            triple = true;
            pos.advance();
            pos.advance();
            // Skip newline after opening """
            if (!pos.done() && pos.peek() == '\n') {
                pos.advance();
            }
        }

        while (!pos.done()) {
            char c = pos.peek();
            if (c == '\\' && !triple) {
                pos.advance();
                if (!pos.done()) {
                    char next = pos.peek();
                    switch (next) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        case '\\': sb.append('\\'); break;
                        case '"': sb.append('"'); break;
                        default: sb.append('\\').append(next); break;
                    }
                    pos.advance();
                }
                continue;
            }
            if (c == quote) {
                pos.advance();
                if (triple) {
                    if (!pos.done() && pos.peek() == quote && pos.i + 1 < pos.text.length() && pos.text.charAt(pos.i + 1) == quote) {
                        pos.advance();
                        pos.advance();
                        break;
                    }
                    // Not closing yet, add the quote
                    sb.append(quote);
                    continue;
                }
                break;
            }
            sb.append(c);
            pos.advance();
        }
        return sb.toString();
    }

    private String parseRawStringValue(Pos pos, String opening) {
        // opening is like #" or ##" etc.
        // Find matching closing: "# or "## etc.
        int hashCount = 0;
        int j = 0;
        while (j < opening.length() && opening.charAt(j) == '#') {
            hashCount++;
            j++;
        }
        // Skip the " after #
        j++; // skip the opening "

        // We already consumed the opening; now read content until closing
        // The closing is: " followed by hashCount #s
        StringBuilder sb = new StringBuilder();
        while (!pos.done()) {
            char c = pos.peek();
            if (c == '"') {
                // Check if this is the closing quote
                int saveI = pos.i;
                pos.advance();
                int count = 0;
                while (!pos.done() && pos.peek() == '#' && count < hashCount) {
                    pos.advance();
                    count++;
                }
                if (count == hashCount) {
                    break;
                }
                // Not the closing, backtrack
                sb.append('"');
                for (int k = 0; k < count; k++) {
                    sb.append('#');
                }
                continue;
            }
            sb.append(c);
            pos.advance();
        }
        return sb.toString();
    }

    // ----- identifier parsing -----

    private String parseIdentifier(Pos pos) {
        pos.skipWs();
        if (pos.done()) return null;

        char c = pos.peek();

        // Quoted identifier: "name" or #"name"#
        if (c == '"') {
            return parseQuotedString(pos, '"');
        }

        // Raw string identifier
        if (c == '#') {
            // Look ahead for #" pattern (raw string)
            if (pos.i + 1 < pos.text.length() && pos.text.charAt(pos.i + 1) == '"') {
                String word = parseWord(pos);
                if (word.startsWith("#") && word.endsWith("\"")) {
                    return parseRawStringValue(pos, word);
                }
                return word;
            }
            // Fall through to parse as word (e.g. #true, #false)
        }

        return parseWord(pos);
    }

    private String parseWord(Pos pos) {
        StringBuilder sb = new StringBuilder();
        while (!pos.done()) {
            char c = pos.peek();
            if (Character.isWhitespace(c) || c == '{' || c == '}' || c == ';' || c == '=' || c == '\\' || c == '/') {
                break;
            }
            // For quoted strings: stop at unescaped "
            if (c == '"') {
                sb.append(c);
                pos.advance();
                // read until closing "
                while (!pos.done()) {
                    char inner = pos.peek();
                    if (inner == '\\') {
                        sb.append(inner);
                        pos.advance();
                        if (!pos.done()) {
                            sb.append(pos.peek());
                            pos.advance();
                        }
                        continue;
                    }
                    sb.append(inner);
                    pos.advance();
                    if (inner == '"') {
                        break;
                    }
                }
                return sb.toString();
            }
            sb.append(c);
            pos.advance();
        }
        return sb.toString();
    }

    // ----- helpers -----

    private void skipRest(Pos pos) {
        while (!pos.done()) {
            char c = pos.peek();
            if (c == '\n' || c == ';' || c == '}') break;
            pos.advance();
        }
        pos.skipWs();
        if (!pos.done() && pos.peek() == ';') {
            pos.advance();
        }
    }

    private static String unquote(String s) {
        if (s == null) return null;
        if (s.length() >= 2) {
            if ((s.startsWith("\"") && s.endsWith("\""))
                    || (s.startsWith("#\"") && s.endsWith("\"#"))) {
                // Already parsed as quoted string value
                return s;
            }
        }
        return s;
    }

    private static ConfigNode toConfigValue(String key, Object value) {
        if (value == null) {
            return new ConfigValue(key, null, ValueType.NULL, null, "");
        }
        if (value instanceof Boolean b) {
            return new ConfigValue(key, b, ValueType.BOOLEAN, null, "");
        }
        if (value instanceof Long l) {
            return new ConfigValue(key, l, ValueType.INTEGER, null, "");
        }
        if (value instanceof Double d) {
            return new ConfigValue(key, d, ValueType.FLOAT, null, "");
        }
        return new ConfigValue(key, value.toString(), ValueType.STRING, null, "");
    }
}
