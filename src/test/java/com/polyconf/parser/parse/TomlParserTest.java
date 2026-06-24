package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TomlParserTest {

    private final TomlParser parser = new TomlParser();

    @Test
    void basicKeyValue() {
        List<String> lines = List.of("name = \"hello\"");
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
        assertEquals("hello", ((ConfigValue) result.children().get("name")).asString().orElseThrow());
    }

    @Test
    void integerValue() {
        List<String> lines = List.of("port = 5432");
        ConfigSection result = parser.parse(lines);

        assertEquals(5432, ((ConfigValue) result.children().get("port")).asInt().orElseThrow());
    }

    @Test
    void booleanValues() {
        List<String> lines = List.of("enabled = true", "debug = false");
        ConfigSection result = parser.parse(lines);

        assertTrue(((ConfigValue) result.children().get("enabled")).asBool().orElseThrow());
        assertFalse(((ConfigValue) result.children().get("debug")).asBool().orElseThrow());
    }

    @Test
    void tableSection() {
        List<String> lines = List.of(
                "[database]",
                "host = \"localhost\"",
                "port = 5432"
        );
        ConfigSection result = parser.parse(lines);

        ConfigSection db = (ConfigSection) result.children().get("database");
        assertEquals("localhost", ((ConfigValue) db.children().get("host")).asString().orElseThrow());
        assertEquals(5432, ((ConfigValue) db.children().get("port")).asInt().orElseThrow());
    }

    @Test
    void nestedTable() {
        List<String> lines = List.of(
                "[database.connection]",
                "host = \"localhost\"",
                "port = 5432"
        );
        ConfigSection result = parser.parse(lines);

        ConfigSection database = (ConfigSection) result.children().get("database");
        ConfigSection connection = (ConfigSection) database.children().get("connection");
        assertEquals("localhost", ((ConfigValue) connection.children().get("host")).asString().orElseThrow());
    }

    @Test
    void arrayValue() {
        List<String> lines = List.of("ports = [8080, 8081, 8082]");
        ConfigSection result = parser.parse(lines);

        ConfigList ports = (ConfigList) result.children().get("ports");
        assertEquals(3, ports.items().size());
    }

    @Test
    void floatValue() {
        List<String> lines = List.of("pi = 3.14");
        ConfigSection result = parser.parse(lines);

        assertEquals(3.14, ((ConfigValue) result.children().get("pi")).asFloat().orElseThrow(), 0.001);
    }

    @Test
    void commentsIgnored() {
        List<String> lines = List.of(
                "# comment",
                "key = \"value\""
        );
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
    void malformedTomlReturnsEmpty() {
        List<String> lines = List.of("= invalid");
        ConfigSection result = parser.parse(lines);

        assertTrue(result.children().isEmpty());
    }

    @Test
    void dottedKey() {
        List<String> lines = List.of("database.host = \"localhost\"");
        ConfigSection result = parser.parse(lines);

        ConfigSection database = (ConfigSection) result.children().get("database");
        assertEquals("localhost", ((ConfigValue) database.children().get("host")).asString().orElseThrow());
    }
}
