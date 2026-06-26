package com.polyconf.parser.format;

import com.polyconf.parser.model.ConfigAccessor;
import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.parse.LenientParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Json5ParserTest {

    private final LenientParser parser = new Json5Format.Parser();

    // --- Core JSON5 features ---

    @Test
    void basicObject() {
        List<String> lines = List.of("{\"name\": \"hello\", \"value\": 42}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(2, result.children().size());
        assertEquals("hello", result.childValue("name").orElseThrow().asString().orElseThrow());
        assertEquals(42, result.childValue("value").orElseThrow().asInt().orElseThrow());
    }

    @Test
    void unquotedKeys() {
        List<String> lines = List.of("{name: \"hello\", value: 42}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("hello", result.childValue("name").orElseThrow().asString().orElseThrow());
        assertEquals(42, result.childValue("value").orElseThrow().asInt().orElseThrow());
    }

    @Test
    void trailingCommas() {
        List<String> lines = List.of("{name: \"hello\", value: 42,}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(2, result.children().size());
        assertEquals("hello", result.childValue("name").orElseThrow().asString().orElseThrow());
        assertEquals(42, result.childValue("value").orElseThrow().asInt().orElseThrow());
    }

    @Test
    void singleLineComments() {
        List<String> lines = List.of("{\n  // This is a comment\n  name: \"hello\",\n  // Another comment\n  value: 42\n}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(2, result.children().size());
        assertEquals("hello", result.childValue("name").orElseThrow().asString().orElseThrow());
        assertEquals(42, result.childValue("value").orElseThrow().asInt().orElseThrow());
    }

    @Test
    void multiLineComments() {
        List<String> lines = List.of("{\n  /* block\n     comment */\n  name: \"hello\",\n  value: 42\n}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(2, result.children().size());
        assertEquals("hello", result.childValue("name").orElseThrow().asString().orElseThrow());
        assertEquals(42, result.childValue("value").orElseThrow().asInt().orElseThrow());
    }

    @Test
    void singleQuotedStrings() {
        List<String> lines = List.of("{name: 'hello world', flag: 'active'}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("hello world", result.childValue("name").orElseThrow().asString().orElseThrow());
        assertEquals("active", result.childValue("flag").orElseThrow().asString().orElseThrow());
    }

    @Test
    void booleanAndNull() {
        List<String> lines = List.of("{active: true, data: null, inactive: false}");
        ConfigSection result = parser.parse(lines).section();

        assertTrue(result.childValue("active").orElseThrow().asBool().orElseThrow());
        assertTrue(result.childValue("data").orElseThrow().isNull());
        assertFalse(result.childValue("inactive").orElseThrow().asBool().orElseThrow());
    }

    @Test
    void hexNumbers() {
        List<String> lines = List.of("{value: 0xFF}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(255, result.childValue("value").orElseThrow().asInt().orElseThrow());
    }

    @Test
    void positiveInfinity() {
        List<String> lines = List.of("{value: Infinity}");
        ConfigSection result = parser.parse(lines).section();

        double v = result.childValue("value").orElseThrow().asFloat().orElseThrow();
        assertEquals(Double.POSITIVE_INFINITY, v);
    }

    @Test
    void negativeInfinity() {
        List<String> lines = List.of("{value: -Infinity}");
        ConfigSection result = parser.parse(lines).section();

        double v = result.childValue("value").orElseThrow().asFloat().orElseThrow();
        assertEquals(Double.NEGATIVE_INFINITY, v);
    }

    // --- Nested structures ---

    @Test
    void nestedObject() {
        List<String> lines = List.of("{db: {host: 'localhost', port: 5432}}");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection dbSection = result.childSection("db").orElseThrow();
        assertEquals("localhost", dbSection.childValue("host").orElseThrow().asString().orElseThrow());
        assertEquals(5432, dbSection.childValue("port").orElseThrow().asInt().orElseThrow());
    }

    @Test
    void arrayValue() {
        List<String> lines = List.of("{items: [1, 2, 3]}");
        ConfigSection result = parser.parse(lines).section();

        ConfigList list = result.childList("items").orElseThrow();
        assertEquals(3, list.items().size());
        assertEquals(1, ((ConfigValue) list.items().get(0)).asInt().orElseThrow());
        assertEquals(3, ((ConfigValue) list.items().get(2)).asInt().orElseThrow());
    }

    @Test
    void arrayTrailingComma() {
        List<String> lines = List.of("[1, 2, 3,]");
        ParserResult pr = parser.parse(lines);

        ConfigList root = pr.section().childList("root").orElseThrow();
        assertEquals(3, root.items().size());
    }

    @Test
    void rootArray() {
        List<String> lines = List.of("[1, 2, 3]");
        ParserResult pr = parser.parse(lines);

        ConfigList root = pr.section().childList("root").orElseThrow();
        assertEquals(3, root.items().size());
    }

    @Test
    void rootArrayOfObjects() {
        List<String> lines = List.of("[{name: 'a'}, {name: 'b'}]");
        ParserResult pr = parser.parse(lines);

        ConfigList root = pr.section().childList("root").orElseThrow();
        assertEquals(2, root.items().size());
        ConfigSection first = (ConfigSection) root.items().get(0);
        assertEquals("a", first.childValue("name").orElseThrow().asString().orElseThrow());
    }

    // --- Edge cases ---

    @Test
    void emptyInput() {
        ConfigSection result = parser.parse(List.of("   ")).section();
        assertTrue(result.children().isEmpty());
    }

    @Test
    void nullInput() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }

    @Test
    void parseError() {
        List<String> lines = List.of("{invalid: ,}");
        ParserResult pr = parser.parse(lines);

        assertEquals(1, pr.diagnostics().size());
        assertTrue(pr.diagnostics().get(0).message().startsWith("JSON5 parse error"));
    }

    @Test
    void flattenedArrayOfObjectsShouldSkipInternalKeys() {
        String json5 = "{ records: [ { id: 1, name: 'Alice' }, { id: 2, name: 'Bob' } ] }";
        ParserResult pr = parser.parse(List.of(json5));
        var f = new ConfigAccessor(pr.section()).asFlattenedMap();
        assertEquals("1", f.get("records[0].id"));
        assertEquals("Alice", f.get("records[0].name"));
        assertEquals("2", f.get("records[1].id"));
        assertEquals("Bob", f.get("records[1].name"));
    }

    @Test
    void commentInsideString() {
        List<String> lines = List.of("{url: 'https://example.com'}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("https://example.com", result.childValue("url").orElseThrow().asString().orElseThrow());
    }

    @Test
    void multilineJson5() {
        List<String> lines = List.of(
                "{",
                "  key: 'value',",
                "}"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals(1, result.children().size());
        assertEquals("value", result.childValue("key").orElseThrow().asString().orElseThrow());
    }

    @Test
    void complexJson5Document() {
        List<String> lines = List.of(
                "{",
                "  // records array",
                "  records: [",
                "    { id: 1, name: 'Alice', active: true, tags: ['admin', 'user'], },",
                "    { id: 2, name: 'Bob', active: false, tags: ['user'], },",
                "  ],",
                "  metadata: {",
                "    exported: '2024-01-15T10:30:00Z',",
                "    count: 3,",
                "  },",
                "}"
        );
        ConfigSection result = parser.parse(lines).section();

        // records array
        ConfigList records = result.childList("records").orElseThrow();
        assertEquals(2, records.items().size());

        ConfigSection first = (ConfigSection) records.items().get(0);
        assertEquals(1, first.childValue("id").orElseThrow().asInt().orElseThrow());
        assertEquals("Alice", first.childValue("name").orElseThrow().asString().orElseThrow());
        assertTrue(first.childValue("active").orElseThrow().asBool().orElseThrow());

        ConfigList firstTags = first.childList("tags").orElseThrow();
        assertEquals(2, firstTags.items().size());

        // metadata
        ConfigSection metadata = result.childSection("metadata").orElseThrow();
        assertEquals("2024-01-15T10:30:00Z", metadata.childValue("exported").orElseThrow().asString().orElseThrow());
        assertEquals(3, metadata.childValue("count").orElseThrow().asInt().orElseThrow());
    }
}
