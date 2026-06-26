package com.polyconf.parser.format;

import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.parse.LenientParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesParserTest {

    private final LenientParser parser = new PropertiesFormat.Parser();

    @Test
    void basicKeyValue() {
        List<String> lines = List.of("key=value");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(1, result.children().size());
        assertEquals("value", result.childValue("key").orElseThrow().asString().orElseThrow());
    }

    @Test
    void keyColonValue() {
        List<String> lines = List.of("key: value");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("value", result.childValue("key").orElseThrow().asString().orElseThrow());
    }

    @Test
    void multipleEntries() {
        List<String> lines = List.of(
                "host=localhost",
                "port=5432",
                "debug=true"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals(3, result.children().size());
        assertEquals("localhost", result.childValue("host").orElseThrow().asString().orElseThrow());
        assertEquals("5432", result.childValue("port").orElseThrow().asString().orElseThrow());
        assertEquals("true", result.childValue("debug").orElseThrow().asString().orElseThrow());
    }


    @Test
    void lineWithoutSeparatorSkipped() {
        List<String> lines = List.of(
                "just a line without separator",
                "key=value"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals(1, result.children().size());
    }

    @Test
    void spacesAroundSeparator() {
        List<String> lines = List.of("key = value");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("value", result.childValue("key").orElseThrow().asString().orElseThrow());
    }


    @Test
    void valueCanContainSpaces() {
        List<String> lines = List.of("message = Hello World");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("Hello World", result.childValue("message").orElseThrow().asString().orElseThrow());
    }

    @Test
    void valueCanContainEquals() {
        List<String> lines = List.of("url=http://example.com?a=1&b=2");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("http://example.com?a=1&b=2", result.childValue("url").orElseThrow().asString().orElseThrow());
    }

    @Test
    void lineContinuation() {
        List<String> lines = List.of(
                "message = Hello \\",
                "  World"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals("Hello   World", result.childValue("message").orElseThrow().asString().orElseThrow());
    }

    @Test
    void dotNotation() {
        List<String> lines = List.of("server.host=localhost", "server.port=5432");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection server = result.childSection("server").orElseThrow();
        assertEquals("localhost", server.childValue("host").orElseThrow().asString().orElseThrow());
        assertEquals("5432", server.childValue("port").orElseThrow().asString().orElseThrow());
    }

    @Test
    void deepDotNotation() {
        List<String> lines = List.of("a.b.c=value");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection a = result.childSection("a").orElseThrow();
        ConfigSection b = a.childSection("b").orElseThrow();
        assertEquals("value", b.childValue("c").orElseThrow().asString().orElseThrow());
    }

    @Test
    void dotNotationCollisionValueSwallowedBySection() {
        // a=1 then a.b=2: the flat value 'a' is silently overwritten by the nested section
        List<String> lines = List.of(
                "a=hello",
                "a.b=world"
        );
        ConfigSection result = parser.parse(lines).section();
        // 'a' should now be a section, not a value; 'hello' is lost
        ConfigSection aSection = result.childSection("a").orElseThrow();
        assertEquals("world", aSection.childValue("b").orElseThrow().asString().orElseThrow());
    }

    @Test
    void dotNotationSectionThenValue() {
        // a.b=1 then a=2: section 'a' is overwritten by a flat value
        List<String> lines = List.of(
                "a.b=nested",
                "a=flat"
        );
        ConfigSection result = parser.parse(lines).section();
        assertEquals("flat", result.childValue("a").orElseThrow().asString().orElseThrow());
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

