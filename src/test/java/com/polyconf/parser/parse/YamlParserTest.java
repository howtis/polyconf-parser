package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParserResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlParserTest {

    private final YamlParser parser = new YamlParser();

    @Test
    void basicKeyValue() {
        List<String> lines = List.of("name: hello");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(1, result.children().size());
        assertEquals("hello", ((ConfigValue) result.children().get("name")).asString().orElseThrow());
    }

    @Test
    void integerValue() {
        List<String> lines = List.of("port: 5432");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(5432, ((ConfigValue) result.children().get("port")).asInt().orElseThrow());
    }

    @Test
    void booleanValues() {
        List<String> lines = List.of("enabled: true", "debug: false");
        ConfigSection result = parser.parse(lines).section();

        assertTrue(((ConfigValue) result.children().get("enabled")).asBool().orElseThrow());
        assertFalse(((ConfigValue) result.children().get("debug")).asBool().orElseThrow());
    }

    @Test
    void nestedMapping() {
        List<String> lines = List.of(
                "database:",
                "  host: localhost",
                "  port: 5432"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigSection db = (ConfigSection) result.children().get("database");
        assertEquals("localhost", ((ConfigValue) db.children().get("host")).asString().orElseThrow());
        assertEquals(5432, ((ConfigValue) db.children().get("port")).asInt().orElseThrow());
    }

    @Test
    void listValue() {
        List<String> lines = List.of(
                "items:",
                "  - a",
                "  - b",
                "  - c"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigList items = (ConfigList) result.children().get("items");
        assertEquals(3, items.items().size());
        assertEquals("a", ((ConfigValue) items.items().get(0)).asString().orElseThrow());
    }

    @Test
    void nullValue() {
        List<String> lines = List.of("key:");
        ConfigSection result = parser.parse(lines).section();

        assertTrue(((ConfigValue) result.children().get("key")).isNull());
    }

    @Test
    void floatValue() {
        List<String> lines = List.of("pi: 3.14");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(3.14, ((ConfigValue) result.children().get("pi")).asFloat().orElseThrow(), 0.001);
    }

    @Test
    void multiDocument() {
        List<String> lines = List.of(
                "key1: value1",
                "---",
                "key2: value2"
        );
        ParserResult pr = parser.parse(lines);

        assertEquals(2, pr.section().children().size());
        ConfigSection doc0 = (ConfigSection) pr.section().children().get("0");
        ConfigSection doc1 = (ConfigSection) pr.section().children().get("1");
        assertEquals("value1", ((ConfigValue) doc0.children().get("key1")).asString().orElseThrow());
        assertEquals("value2", ((ConfigValue) doc1.children().get("key2")).asString().orElseThrow());
    }

    @Test
    void rootList() {
        List<String> lines = List.of(
                "- item1",
                "- item2",
                "- item3"
        );
        ParserResult pr = parser.parse(lines);

        ConfigList root = (ConfigList) pr.section().children().get("root");
        assertEquals(3, root.items().size());
        assertEquals("item1", ((ConfigValue) root.items().get(0)).asString().orElseThrow());
    }

    @Test
    void anchorCycleDetected() {
        List<String> lines = List.of(
                "a: &anchor",
                "  b: *anchor"
        );
        ParserResult pr = parser.parse(lines);

        assertEquals(1, pr.diagnostics().size());
        assertEquals(DiagnosticLevel.ERROR, pr.diagnostics().get(0).level());
        assertTrue(pr.diagnostics().get(0).message().contains("circular anchor"));
    }
}
