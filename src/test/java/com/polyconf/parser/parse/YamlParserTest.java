package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlParserTest {

    private final YamlParser parser = new YamlParser();

    @Test
    void basicKeyValue() {
        List<String> lines = List.of("name: hello");
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
        assertEquals("hello", ((ConfigValue) result.children().get("name")).asString().orElseThrow());
    }

    @Test
    void integerValue() {
        List<String> lines = List.of("port: 5432");
        ConfigSection result = parser.parse(lines);

        assertEquals(5432, ((ConfigValue) result.children().get("port")).asInt().orElseThrow());
    }

    @Test
    void booleanValues() {
        List<String> lines = List.of("enabled: true", "debug: false");
        ConfigSection result = parser.parse(lines);

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
        ConfigSection result = parser.parse(lines);

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
        ConfigSection result = parser.parse(lines);

        ConfigList items = (ConfigList) result.children().get("items");
        assertEquals(3, items.items().size());
        assertEquals("a", ((ConfigValue) items.items().get(0)).asString().orElseThrow());
    }

    @Test
    void nullValue() {
        List<String> lines = List.of("key:");
        ConfigSection result = parser.parse(lines);

        assertTrue(((ConfigValue) result.children().get("key")).isNull());
    }

    @Test
    void floatValue() {
        List<String> lines = List.of("pi: 3.14");
        ConfigSection result = parser.parse(lines);

        assertEquals(3.14, ((ConfigValue) result.children().get("pi")).asFloat().orElseThrow(), 0.001);
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
    void malformedYamlReturnsEmpty() {
        List<String> lines = List.of(": invalid");
        ConfigSection result = parser.parse(lines);

        assertTrue(result.children().isEmpty());
    }

    @Test
    void commentsIgnored() {
        List<String> lines = List.of(
                "# comment",
                "key: value"
        );
        ConfigSection result = parser.parse(lines);

        assertEquals("value", ((ConfigValue) result.children().get("key")).asString().orElseThrow());
    }
}
