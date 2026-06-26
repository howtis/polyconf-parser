package com.polyconf.parser.format;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.parse.LenientParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlParserTest {

    private final LenientParser parser = new YamlFormat.Parser();

    @Test
    void basicKeyValue() {
        List<String> lines = List.of("name: hello");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(1, result.children().size());
        assertEquals("hello", result.childValue("name").orElseThrow().asString().orElseThrow());
    }

    @Test
    void integerValue() {
        List<String> lines = List.of("port: 5432");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(5432, result.childValue("port").orElseThrow().asInt().orElseThrow());
    }

    @Test
    void booleanValues() {
        List<String> lines = List.of("enabled: true", "debug: false");
        ConfigSection result = parser.parse(lines).section();

        assertTrue(result.childValue("enabled").orElseThrow().asBool().orElseThrow());
        assertFalse(result.childValue("debug").orElseThrow().asBool().orElseThrow());
    }

    @Test
    void nestedMapping() {
        List<String> lines = List.of(
                "database:",
                "  host: localhost",
                "  port: 5432"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigSection db = result.childSection("database").orElseThrow();
        assertEquals("localhost", db.childValue("host").orElseThrow().asString().orElseThrow());
        assertEquals(5432, db.childValue("port").orElseThrow().asInt().orElseThrow());
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

        ConfigList items = result.childList("items").orElseThrow();
        assertEquals(3, items.items().size());
        assertEquals("a", ((ConfigValue) items.items().get(0)).asString().orElseThrow());
    }

    @Test
    void nullValue() {
        List<String> lines = List.of("key:");
        ConfigSection result = parser.parse(lines).section();

        assertTrue(result.childValue("key").orElseThrow().isNull());
    }

    @Test
    void floatValue() {
        List<String> lines = List.of("pi: 3.14");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(3.14, result.childValue("pi").orElseThrow().asFloat().orElseThrow(), 0.001);
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
        ConfigSection doc0 = pr.section().childSection("0").orElseThrow();
        ConfigSection doc1 = pr.section().childSection("1").orElseThrow();
        assertEquals("value1", doc0.childValue("key1").orElseThrow().asString().orElseThrow());
        assertEquals("value2", doc1.childValue("key2").orElseThrow().asString().orElseThrow());
    }

    @Test
    void rootList() {
        List<String> lines = List.of(
                "- item1",
                "- item2",
                "- item3"
        );
        ParserResult pr = parser.parse(lines);

        ConfigList root = pr.section().childList("root").orElseThrow();
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

    @Test
    void emptyInput() {
        ConfigSection result = parser.parse(List.of("   ")).section();
        assertTrue(result.children().isEmpty());
    }

    @Test
    void nullInput() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }
}
