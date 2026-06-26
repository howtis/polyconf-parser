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

        KdlDocument doc;
        try {
            doc = PARSER.parse(text);
        } catch (Exception e) {
            return new ParserResult(
                    new ConfigSection("", null, ""),
                    List.of(new BlockDiagnostic(0, lines.size() - 1,
                            "KDL parse error: " + e.getMessage(),
                            DiagnosticLevel.ERROR))
            );
        }

        Map<String, ConfigNode> children = new LinkedHashMap<>();
        for (KdlNode node : doc.nodes()) {
            ConfigNode converted = convertNode(node);
            String name = node.name();
            if (children.containsKey(name)) {
                int idx = 1;
                while (children.containsKey(name + "_" + idx)) {
                    idx++;
                }
                children.put(name + "_" + idx, converted);
            } else {
                children.put(name, converted);
            }
        }

        return ParserResult.ok(new ConfigSection("", children, null, ""));
    }

    private ConfigNode convertNode(KdlNode node) {
        Map<String, ConfigNode> contents = new LinkedHashMap<>();

        List<KdlValue<?>> args = node.arguments();
        for (int i = 0; i < args.size(); i++) {
            contents.put(String.valueOf(i), convertValue(String.valueOf(i), args.get(i)));
        }

        KdlProperties props = node.properties();
        if (props != null) {
            for (Map.Entry<String, List<KdlValue<?>>> prop : props) {
                List<KdlValue<?>> values = prop.getValue();
                if (!values.isEmpty()) {
                    contents.put(prop.getKey(), convertValue(prop.getKey(), values.get(0)));
                }
            }
        }

        for (KdlNode childNode : node.children()) {
            ConfigNode child = convertNode(childNode);
            String childName = childNode.name();
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

        return new ConfigSection(node.name(), contents, null, node.name());
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
