package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.ParserResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesParserTest {

    private final PropertiesParser parser = new PropertiesParser();

    @Test
    void basicKeyValue() {
        List<String> lines = List.of("key=value");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(1, result.children().size());
        ConfigNode node = result.children().get("key");
        assertInstanceOf(ConfigValue.class, node);
        assertEquals("value", ((ConfigValue) node).asString().orElseThrow());
    }

    @Test
    void keyColonValue() {
        List<String> lines = List.of("key: value");
        ConfigSection result = parser.parse(lines).section();

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
        ConfigSection result = parser.parse(lines).section();

        assertEquals(3, result.children().size());
        assertEquals("localhost", ((ConfigValue) result.children().get("host")).asString().orElseThrow());
        assertEquals("5432", ((ConfigValue) result.children().get("port")).asString().orElseThrow());
        assertEquals("true", ((ConfigValue) result.children().get("debug")).asString().orElseThrow());
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

        assertEquals("value", ((ConfigValue) result.children().get("key")).asString().orElseThrow());
    }


    @Test
    void valueCanContainSpaces() {
        List<String> lines = List.of("message = Hello World");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("Hello World", ((ConfigValue) result.children().get("message")).asString().orElseThrow());
    }

    @Test
    void valueCanContainEquals() {
        List<String> lines = List.of("url=http://example.com?a=1&b=2");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("http://example.com?a=1&b=2", ((ConfigValue) result.children().get("url")).asString().orElseThrow());
    }

    @Test
    void lineContinuation() {
        List<String> lines = List.of(
                "message = Hello \\",
                "  World"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals("Hello   World", ((ConfigValue) result.children().get("message")).asString().orElseThrow());
    }

    @Test
    void dotNotation() {
        List<String> lines = List.of("server.host=localhost", "server.port=5432");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection server = (ConfigSection) result.children().get("server");
        assertEquals("localhost", ((ConfigValue) server.children().get("host")).asString().orElseThrow());
        assertEquals("5432", ((ConfigValue) server.children().get("port")).asString().orElseThrow());
    }

    @Test
    void deepDotNotation() {
        List<String> lines = List.of("a.b.c=value");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection a = (ConfigSection) result.children().get("a");
        ConfigSection b = (ConfigSection) a.children().get("b");
        assertEquals("value", ((ConfigValue) b.children().get("c")).asString().orElseThrow());
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
        ConfigNode a = result.children().get("a");
        assertInstanceOf(ConfigSection.class, a);
        ConfigSection aSection = (ConfigSection) a;
        assertEquals("world", ((ConfigValue) aSection.children().get("b")).asString().orElseThrow());
    }

    @Test
    void dotNotationSectionThenValue() {
        // a.b=1 then a=2: section 'a' is overwritten by a flat value
        List<String> lines = List.of(
                "a.b=nested",
                "a=flat"
        );
        ConfigSection result = parser.parse(lines).section();
        ConfigNode a = result.children().get("a");
        assertInstanceOf(ConfigValue.class, a);
        assertEquals("flat", ((ConfigValue) a).asString().orElseThrow());
    }
}

