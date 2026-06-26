package com.polyconf.parser.parse;

import com.polyconf.parser.format.Json5Format;
import com.polyconf.parser.model.ConfigAccessor;
import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
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
        assertEquals("hello", ((ConfigValue) result.children().get("name")).asString().orElseThrow());
        assertEquals(42, ((ConfigValue) result.children().get("value")).asInt().orElseThrow());
    }

    @Test
    void unquotedKeys() {
        List<String> lines = List.of("{name: \"hello\", value: 42}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("hello", ((ConfigValue) result.children().get("name")).asString().orElseThrow());
        assertEquals(42, ((ConfigValue) result.children().get("value")).asInt().orElseThrow());
    }

    @Test
    void trailingCommas() {
        List<String> lines = List.of("{name: \"hello\", value: 42,}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(2, result.children().size());
        assertEquals("hello", ((ConfigValue) result.children().get("name")).asString().orElseThrow());
        assertEquals(42, ((ConfigValue) result.children().get("value")).asInt().orElseThrow());
    }

    @Test
    void singleLineComments() {
        List<String> lines = List.of("{\n  // This is a comment\n  name: \"hello\",\n  // Another comment\n  value: 42\n}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(2, result.children().size());
        assertEquals("hello", ((ConfigValue) result.children().get("name")).asString().orElseThrow());
        assertEquals(42, ((ConfigValue) result.children().get("value")).asInt().orElseThrow());
    }

    @Test
    void multiLineComments() {
        List<String> lines = List.of("{\n  /* block\n     comment */\n  name: \"hello\",\n  value: 42\n}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(2, result.children().size());
        assertEquals("hello", ((ConfigValue) result.children().get("name")).asString().orElseThrow());
        assertEquals(42, ((ConfigValue) result.children().get("value")).asInt().orElseThrow());
    }

    @Test
    void singleQuotedStrings() {
        List<String> lines = List.of("{name: 'hello world', flag: 'active'}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("hello world", ((ConfigValue) result.children().get("name")).asString().orElseThrow());
        assertEquals("active", ((ConfigValue) result.children().get("flag")).asString().orElseThrow());
    }

    @Test
    void booleanAndNull() {
        List<String> lines = List.of("{active: true, data: null, inactive: false}");
        ConfigSection result = parser.parse(lines).section();

        assertTrue(((ConfigValue) result.children().get("active")).asBool().orElseThrow());
        assertTrue(((ConfigValue) result.children().get("data")).isNull());
        assertFalse(((ConfigValue) result.children().get("inactive")).asBool().orElseThrow());
    }

    @Test
    void hexNumbers() {
        List<String> lines = List.of("{value: 0xFF}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(255, ((ConfigValue) result.children().get("value")).asInt().orElseThrow());
    }

    @Test
    void positiveInfinity() {
        List<String> lines = List.of("{value: Infinity}");
        ConfigSection result = parser.parse(lines).section();

        double v = ((ConfigValue) result.children().get("value")).asFloat().orElseThrow();
        assertEquals(Double.POSITIVE_INFINITY, v);
    }

    @Test
    void negativeInfinity() {
        List<String> lines = List.of("{value: -Infinity}");
        ConfigSection result = parser.parse(lines).section();

        double v = ((ConfigValue) result.children().get("value")).asFloat().orElseThrow();
        assertEquals(Double.NEGATIVE_INFINITY, v);
    }

    // --- Nested structures ---

    @Test
    void nestedObject() {
        List<String> lines = List.of("{db: {host: 'localhost', port: 5432}}");
        ConfigSection result = parser.parse(lines).section();

        ConfigNode db = result.children().get("db");
        assertInstanceOf(ConfigSection.class, db);
        ConfigSection dbSection = (ConfigSection) db;
        assertEquals("localhost", ((ConfigValue) dbSection.children().get("host")).asString().orElseThrow());
        assertEquals(5432, ((ConfigValue) dbSection.children().get("port")).asInt().orElseThrow());
    }

    @Test
    void arrayValue() {
        List<String> lines = List.of("{items: [1, 2, 3]}");
        ConfigSection result = parser.parse(lines).section();

        ConfigNode items = result.children().get("items");
        assertInstanceOf(ConfigList.class, items);
        ConfigList list = (ConfigList) items;
        assertEquals(3, list.items().size());
        assertEquals(1, ((ConfigValue) list.items().get(0)).asInt().orElseThrow());
        assertEquals(3, ((ConfigValue) list.items().get(2)).asInt().orElseThrow());
    }

    @Test
    void arrayTrailingComma() {
        List<String> lines = List.of("[1, 2, 3,]");
        ParserResult pr = parser.parse(lines);

        ConfigList root = (ConfigList) pr.section().children().get("root");
        assertEquals(3, root.items().size());
    }

    @Test
    void rootArray() {
        List<String> lines = List.of("[1, 2, 3]");
        ParserResult pr = parser.parse(lines);

        ConfigList root = (ConfigList) pr.section().children().get("root");
        assertEquals(3, root.items().size());
    }

    @Test
    void rootArrayOfObjects() {
        List<String> lines = List.of("[{name: 'a'}, {name: 'b'}]");
        ParserResult pr = parser.parse(lines);

        ConfigList root = (ConfigList) pr.section().children().get("root");
        assertEquals(2, root.items().size());
        ConfigSection first = (ConfigSection) root.items().get(0);
        assertEquals("a", ((ConfigValue) first.children().get("name")).asString().orElseThrow());
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
        assertEquals(1L, f.get("records[0].id"));
        assertEquals("Alice", f.get("records[0].name"));
        assertEquals(2L, f.get("records[1].id"));
        assertEquals("Bob", f.get("records[1].name"));
    }

    @Test
    void commentInsideString() {
        List<String> lines = List.of("{url: 'https://example.com'}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("https://example.com", ((ConfigValue) result.children().get("url")).asString().orElseThrow());
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
        assertEquals("value", ((ConfigValue) result.children().get("key")).asString().orElseThrow());
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
        ConfigList records = (ConfigList) result.children().get("records");
        assertEquals(2, records.items().size());

        ConfigSection first = (ConfigSection) records.items().get(0);
        assertEquals(1, ((ConfigValue) first.children().get("id")).asInt().orElseThrow());
        assertEquals("Alice", ((ConfigValue) first.children().get("name")).asString().orElseThrow());
        assertTrue(((ConfigValue) first.children().get("active")).asBool().orElseThrow());

        ConfigList firstTags = (ConfigList) first.children().get("tags");
        assertEquals(2, firstTags.items().size());

        // metadata
        ConfigSection metadata = (ConfigSection) result.children().get("metadata");
        assertEquals("2024-01-15T10:30:00Z", ((ConfigValue) metadata.children().get("exported")).asString().orElseThrow());
        assertEquals(3, ((ConfigValue) metadata.children().get("count")).asInt().orElseThrow());
    }
}
