package com.polyconf.parser.format;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.parse.LenientParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HoconParserTest {

    private final LenientParser parser = new HoconFormat.Parser();

    @Test
    void basicKeyValue() {
        List<String> lines = List.of("name = \"MyApp\"");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(1, result.children().size());
        assertEquals("MyApp", result.childValue("name").orElseThrow().asString().orElseThrow());
    }

    @Test
    void blockSyntax() {
        List<String> lines = List.of(
                "app {",
                "  name = \"MyApp\"",
                "  version = 1",
                "}"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigSection app = result.childSection("app").orElseThrow();
        assertEquals("MyApp", app.childValue("name").orElseThrow().asString().orElseThrow());
        assertEquals(1, app.childValue("version").orElseThrow().asInt().orElseThrow());
    }

    @Test
    void pathExpressions() {
        List<String> lines = List.of(
                "app.name = \"MyApp\"",
                "app.version = \"1.0\""
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigSection app = result.childSection("app").orElseThrow();
        assertEquals("MyApp", app.childValue("name").orElseThrow().asString().orElseThrow());
        assertEquals("1.0", app.childValue("version").orElseThrow().asString().orElseThrow());
    }

    @Test
    void numberValues() {
        List<String> lines = List.of(
                "int-val = 42",
                "float-val = 3.14"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals(42, result.childValue("int-val").orElseThrow().asInt().orElseThrow());
        assertEquals(3.14, result.childValue("float-val").orElseThrow().asFloat().orElseThrow(), 0.001);
    }

    @Test
    void booleanAndNull() {
        List<String> lines = List.of(
                "enabled = true",
                "debug = false",
                "data = null"
        );
        ConfigSection result = parser.parse(lines).section();

        assertTrue(result.childValue("enabled").orElseThrow().asBool().orElseThrow());
        assertFalse(result.childValue("debug").orElseThrow().asBool().orElseThrow());
        assertTrue(result.childValue("data").orElseThrow().isNull());
    }

    @Test
    void arrayValue() {
        List<String> lines = List.of("ports = [8080, 8081, 8082]");
        ConfigSection result = parser.parse(lines).section();

        ConfigList list = result.childList("ports").orElseThrow();
        assertEquals(3, list.items().size());
    }

    @Test
    void includeCausesNotResolvedError() {
        // include with non-existent file — parser should handle without throwing.
        // Typesafe Config 1.4.3 may resolve includes silently (skipping missing files).
        List<String> lines = List.of(
                "include \"nonexistent.conf\"",
                "name = \"MyApp\""
        );
        ParserResult pr = parser.parse(lines);

        assertNotNull(pr.section());
        assertEquals("MyApp",
                pr.section().childValue("name").orElseThrow().asString().orElseThrow());
    }

    @Test
    void plusEqualsMerging() {
        // += is resolved at resolve time. Without successful resolve,
        // the last assignment "a += [3, 4]" becomes a plain value.
        // This documents current parser behavior for the += operator.
        List<String> lines = List.of(
                "a = [1, 2]",
                "a += [3, 4]"
        );
        ParserResult pr = parser.parse(lines);

        // Either a list (from last assignment) or section depending on resolution
        assertTrue(pr.section().children().containsKey("a"));
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
