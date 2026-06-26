package com.polyconf.parser.parse;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.ValueType;
import dev.kdl.KdlDocument;
import dev.kdl.KdlNode;
import dev.kdl.KdlNumber;
import dev.kdl.KdlProperties;
import dev.kdl.KdlValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KdlParser implements LenientParser {

    private static final dev.kdl.parse.KdlParser PARSER = dev.kdl.parse.KdlParser.hybrid();

    @Override
    public ParserResult parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        String text = String.join("\n", lines);
        if (text.isBlank()) {
            return ParserResult.ok(new ConfigSection("", new LinkedHashMap<>(), null, ""));
        }

        // Quick reject: content starting with { or [ is JSON/JSON5, not KDL
        String stripped = text.strip();
        if (stripped.startsWith("{") || stripped.startsWith("[")) {
            return new ParserResult(
                    new ConfigSection("", new LinkedHashMap<>(), null, ""),
                    List.of()
            );
        }

        Map<String, ConfigNode> children = new LinkedHashMap<>();

        // Use hybrid approach: split into top-level nodes, then parse each
        // using kdl4j for property/argument extraction and manual recursion
        // for children to work around kdl4j child-node parsing bugs.
        for (String nodeText : splitTopLevelNodes(lines)) {
            if (nodeText.isBlank()) {
                continue;
            }
            try {
                ConfigNode parsed = parseNodeText(nodeText);
                String name;
                if (parsed instanceof ConfigValue cv) {
                    name = cv.key();
                } else {
                    name = ((ConfigSection) parsed).key();
                }
                if (children.containsKey(name)) {
                    int idx = 1;
                    while (children.containsKey(name + "_" + idx)) {
                        idx++;
                    }
                    children.put(name + "_" + idx, parsed);
                } else {
                    children.put(name, parsed);
                }
            } catch (Exception e) {
                return new ParserResult(
                        new ConfigSection("", null, ""),
                        List.of(new BlockDiagnostic(0, lines.size() - 1,
                                "KDL parse error: " + e.getMessage(),
                                DiagnosticLevel.ERROR))
                );
            }
        }

        return ParserResult.ok(new ConfigSection("", children, null, ""));
    }

    /**
     * Parses a single KDL node text (possibly with children in braces).
     * Uses kdl4j for header (name + args + props) and manual recursion
     * for children to avoid kdl4j parsing bugs with multiple child nodes.
     */
    private ConfigNode parseNodeText(String text) {
        int braceIdx = findOpenBrace(text);
        String header;
        String childrenText;

        if (braceIdx >= 0) {
            header = text.substring(0, braceIdx).stripTrailing();
            int matchingClose = findMatchingClose(text, braceIdx);
            if (matchingClose >= 0) {
                childrenText = text.substring(braceIdx + 1, matchingClose).strip();
            } else {
                childrenText = text.substring(braceIdx + 1).strip();
            }
        } else {
            header = text.strip();
            childrenText = null;
        }

        // Parse header with kdl4j to get name, arguments, and properties
        KdlNode kdlNode;
        String fallbackValue = null;
        try {
            KdlDocument doc = PARSER.parse(header);
            if (doc.nodes().isEmpty()) {
                // Empty parse, treat as simple node name
                return new ConfigSection(header, new LinkedHashMap<>(), null, header);
            }
            kdlNode = doc.nodes().get(0);
        } catch (Exception e) {
            return new ConfigSection(header, new LinkedHashMap<>(), null, header);
        }

        // Fallback: if kdl4j parsed the header but produced no args/props,
        // and the header has a node name followed by extra text,
        // treat the extra text as a raw string value.
        if (kdlNode.arguments().isEmpty()
                && (kdlNode.properties() == null || !kdlNode.properties().iterator().hasNext())) {
            String nodeName = kdlNode.name();
            int afterName = header.indexOf(nodeName) + nodeName.length();
            if (afterName < header.length()) {
                fallbackValue = header.substring(afterName).strip();
            }
        }

        // Leaf node: single argument, no properties, no children
        List<KdlValue<?>> args = kdlNode.arguments();
        KdlProperties props = kdlNode.properties();
        boolean hasProps = props != null && props.iterator().hasNext();
        boolean hasChildren = childrenText != null && !childrenText.isEmpty();

        if (!hasChildren && !hasProps) {
            if (args.size() == 1) {
                return convertValue(kdlNode.name(), args.get(0));
            }
            if (args.isEmpty() && fallbackValue != null) {
                return new ConfigValue(kdlNode.name(), fallbackValue, ValueType.STRING, null, "");
            }
        }

        Map<String, ConfigNode> contents = new LinkedHashMap<>();

        // Arguments
        for (int i = 0; i < args.size(); i++) {
            contents.put(String.valueOf(i), convertValue(String.valueOf(i), args.get(i)));
        }

        // Properties
        if (props != null) {
            for (Map.Entry<String, List<KdlValue<?>>> prop : props) {
                List<KdlValue<?>> values = prop.getValue();
                if (!values.isEmpty()) {
                    contents.put(prop.getKey(), convertValue(prop.getKey(), values.get(0)));
                }
            }
        }

        // Children: parse manually to avoid kdl4j bugs
        if (hasChildren) {
            List<String> childLines = splitChildNodes(childrenText);
            for (String childLine : childLines) {
                ConfigNode child = parseNodeText(childLine);
                String childName;
                if (child instanceof ConfigValue cv) {
                    childName = cv.key();
                } else {
                    childName = ((ConfigSection) child).key();
                }
                if (contents.containsKey(childName)) {
                    int idx = 1;
                    while (contents.containsKey(childName + "_" + idx)) {
                        idx++;
                    }
                    contents.put(childName + "_" + idx, child);
                } else {
                    contents.put(childName, child);
                }
            }
        }

        return new ConfigSection(kdlNode.name(), contents, null, kdlNode.name());
    }

    /**
     * Splits children text into individual child node texts,
     * respecting nested brace depth.
     */
    private static List<String> splitChildNodes(String childrenText) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceDepth = 0;
        boolean inString = false;

        for (String line : childrenText.split("\n")) {
            String stripped = line.strip();
            if (stripped.isEmpty() || isKdlComment(stripped)) {
                continue;
            }

            for (int i = 0; i < stripped.length(); i++) {
                char c = stripped.charAt(i);
                if (c == '"') {
                    inString = !inString;
                } else if (c == '{' && !inString) {
                    braceDepth++;
                } else if (c == '}' && !inString) {
                    braceDepth--;
                }
            }

            if (braceDepth == 0 && current.length() > 0) {
                // Previous child completed, this is a new child
                result.add(current.toString());
                current.setLength(0);
            }

            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(stripped);
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    /**
     * Finds the matching closing brace for the opening brace at braceIdx,
     * respecting nested braces and quoted strings.
     */
    private static int findMatchingClose(String text, int openIdx) {
        int depth = 1;
        boolean inString = false;
        for (int i = openIdx + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (c == '{' && !inString) {
                depth++;
            } else if (c == '}' && !inString) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findOpenBrace(String text) {
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (c == '{' && !inString) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Splits lines into individual KDL node texts, respecting brace depth.
     * A top-level node ends when brace depth returns to 0 after an opening brace,
     * or at the end of a single-line node without braces.
     */
    public static List<String> splitTopLevelNodes(List<String> lines) {
        List<String> nodes = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceDepth = 0;
        boolean insideNode = false;

        for (String line : lines) {
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '{') {
                    braceDepth++;
                } else if (c == '}') {
                    braceDepth--;
                }
            }

            if (!insideNode) {
                if (line.isBlank() || isKdlComment(line)) {
                    continue;
                }
                insideNode = true;
            }

            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(line);

            if (insideNode && braceDepth == 0) {
                nodes.add(current.toString());
                current.setLength(0);
                insideNode = false;
            }
        }

        if (current.length() > 0) {
            nodes.add(current.toString());
        }

        // Split semicolon-separated nodes
        List<String> result = new ArrayList<>();
        for (String nodeText : nodes) {
            if (nodeText.indexOf(';') >= 0 && nodeText.indexOf('{') < 0) {
                for (String part : nodeText.split(";")) {
                    String trimmed = part.strip();
                    if (!trimmed.isEmpty()) {
                        result.add(trimmed);
                    }
                }
            } else {
                result.add(nodeText);
            }
        }

        return result;
    }

    private static boolean isKdlComment(String line) {
        String stripped = line.strip();
        return stripped.startsWith("//") || stripped.startsWith("/*");
    }

    private static ConfigNode convertValue(String key, KdlValue<?> value) {
        if (value.isNull()) {
            return new ConfigValue(key, null, ValueType.NULL, null, "");
        }
        if (value.isBoolean()) {
            return new ConfigValue(key, value.value(), ValueType.BOOLEAN, null, "");
        }
        if (value.isNumber()) {
            KdlNumber<?> num = (KdlNumber<?>) value;
            if (num instanceof KdlNumber.Integer) {
                return new ConfigValue(key, num.asLong(), ValueType.INTEGER, null, "");
            }
            return new ConfigValue(key, num.asBigDecimal().doubleValue(), ValueType.FLOAT, null, "");
        }
        return new ConfigValue(key, value.value().toString(), ValueType.STRING, null, "");
    }
}
