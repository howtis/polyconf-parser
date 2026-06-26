package com.polyconf.parser.format;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.parse.LenientParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonParserTest {

    private final LenientParser parser = new JsonFormat.Parser();

    @Test
    void basicObject() {
        List<String> lines = List.of("{\"name\": \"hello\", \"value\": 42}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(2, result.children().size());
        assertEquals("hello", result.childValue("name").orElseThrow().asString().orElseThrow());
        assertEquals(42, result.childValue("value").orElseThrow().asInt().orElseThrow());
    }

    @Test
    void booleanAndNull() {
        List<String> lines = List.of("{\"active\": true, \"data\": null}");
        ConfigSection result = parser.parse(lines).section();

        assertTrue(result.childValue("active").orElseThrow().asBool().orElseThrow());
        assertTrue(result.childValue("data").orElseThrow().isNull());
    }

    @Test
    void nestedObject() {
        List<String> lines = List.of("{\"db\": {\"host\": \"localhost\", \"port\": 5432}}");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection dbSection = result.childSection("db").orElseThrow();
        assertEquals("localhost", dbSection.childValue("host").orElseThrow().asString().orElseThrow());
        assertEquals(5432, dbSection.childValue("port").orElseThrow().asInt().orElseThrow());
    }

    @Test
    void arrayValue() {
        List<String> lines = List.of("{\"items\": [1, 2, 3]}");
        ConfigSection result = parser.parse(lines).section();

        ConfigList list = result.childList("items").orElseThrow();
        assertEquals(3, list.items().size());
        assertEquals(1, ((ConfigValue) list.items().get(0)).asInt().orElseThrow());
        assertEquals(2, ((ConfigValue) list.items().get(1)).asInt().orElseThrow());
        assertEquals(3, ((ConfigValue) list.items().get(2)).asInt().orElseThrow());
    }

    @Test
    void nestedArray() {
        List<String> lines = List.of("{\"matrix\": [[1, 2], [3, 4]]}");
        ConfigSection result = parser.parse(lines).section();

        assertNotNull(result.childList("matrix").orElse(null));
    }

    @Test
    void floatValue() {
        List<String> lines = List.of("{\"pi\": 3.14}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(3.14, result.childValue("pi").orElseThrow().asFloat().orElseThrow(), 0.001);
    }

    @Test
    void multilineJson() {
        List<String> lines = List.of(
                "{",
                "  \"key\": \"value\"",
                "}"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals(1, result.children().size());
        assertEquals("value", result.childValue("key").orElseThrow().asString().orElseThrow());
    }

    @Test
    void rootArray() {
        List<String> lines = List.of("[1, 2, 3]");
        ParserResult pr = parser.parse(lines);

        ConfigList root = pr.section().childList("root").orElseThrow();
        assertEquals(3, root.items().size());
        assertEquals(1, ((ConfigValue) root.items().get(0)).asInt().orElseThrow());
        assertEquals(2, ((ConfigValue) root.items().get(1)).asInt().orElseThrow());
        assertEquals(3, ((ConfigValue) root.items().get(2)).asInt().orElseThrow());
    }

    @Test
    void rootArrayOfObjects() {
        List<String> lines = List.of("[{\"name\": \"a\"}, {\"name\": \"b\"}]");
        ParserResult pr = parser.parse(lines);

        ConfigList root = pr.section().childList("root").orElseThrow();
        assertEquals(2, root.items().size());
        ConfigSection first = (ConfigSection) root.items().get(0);
        assertEquals("a", first.childValue("name").orElseThrow().asString().orElseThrow());
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
