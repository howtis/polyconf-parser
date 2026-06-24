package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesParserTest {

    private final PropertiesParser parser = new PropertiesParser();

    @Test
    void basicKeyValue() {
        List<String> lines = List.of("key=value");
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
        ConfigNode node = result.children().get("key");
        assertInstanceOf(ConfigValue.class, node);
        assertEquals("value", ((ConfigValue) node).asString().orElseThrow());
    }

    @Test
    void keyColonValue() {
        List<String> lines = List.of("key: value");
        ConfigSection result = parser.parse(lines);

        ConfigNode node = result.children().get("key");
        assertNotNull(node);
        assertEquals("value", ((ConfigValue) node).asString().orElseThrow());
    }

    @Test
    void multipleEntries() {
        List<String> lines = List.of(
                "host=localhost",
                "port=5432",
                "debug=true"
        );
        ConfigSection result = parser.parse(lines);

        assertEquals(3, result.children().size());
        assertEquals("localhost", ((ConfigValue) result.children().get("host")).asString().orElseThrow());
        assertEquals("5432", ((ConfigValue) result.children().get("port")).asString().orElseThrow());
        assertEquals("true", ((ConfigValue) result.children().get("debug")).asString().orElseThrow());
    }

    @Test
    void hashCommentsIgnored() {
        List<String> lines = List.of(
                "# this is a comment",
                "key=value"
        );
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
        assertEquals("value", ((ConfigValue) result.children().get("key")).asString().orElseThrow());
    }

    @Test
    void exclamationCommentsIgnored() {
        List<String> lines = List.of(
                "! this is a comment",
                "key=value"
        );
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
    }

    @Test
    void emptyLinesSkipped() {
        List<String> lines = List.of(
                "",
                "key=value",
                "",
                ""
        );
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
    }

    @Test
    void lineWithoutSeparatorSkipped() {
        List<String> lines = List.of(
                "just a line without separator",
                "key=value"
        );
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
    }

    @Test
    void spacesAroundSeparator() {
        List<String> lines = List.of("key = value");
        ConfigSection result = parser.parse(lines);

        assertEquals("value", ((ConfigValue) result.children().get("key")).asString().orElseThrow());
    }

    @Test
    void emptyInput() {
        ConfigSection result = parser.parse(List.of());
        assertTrue(result.children().isEmpty());
    }

    @Test
    void nullInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }

    @Test
    void valueCanContainSpaces() {
        List<String> lines = List.of("message = Hello World");
        ConfigSection result = parser.parse(lines);

        assertEquals("Hello World", ((ConfigValue) result.children().get("message")).asString().orElseThrow());
    }

    @Test
    void valueCanContainEquals() {
        List<String> lines = List.of("url=http://example.com?a=1&b=2");
        ConfigSection result = parser.parse(lines);

        assertEquals("http://example.com?a=1&b=2", ((ConfigValue) result.children().get("url")).asString().orElseThrow());
    }
}
