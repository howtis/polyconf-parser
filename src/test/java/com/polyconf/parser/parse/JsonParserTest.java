package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonParserTest {

    private final JsonParser parser = new JsonParser();

    @Test
    void basicObject() {
        List<String> lines = List.of("{\"name\": \"hello\", \"value\": 42}");
        ConfigSection result = parser.parse(lines);

        assertEquals(2, result.children().size());
        assertEquals("hello", ((ConfigValue) result.children().get("name")).asString().orElseThrow());
        assertEquals(42, ((ConfigValue) result.children().get("value")).asInt().orElseThrow());
    }

    @Test
    void booleanAndNull() {
        List<String> lines = List.of("{\"active\": true, \"data\": null}");
        ConfigSection result = parser.parse(lines);

        assertTrue(((ConfigValue) result.children().get("active")).asBool().orElseThrow());
        assertTrue(((ConfigValue) result.children().get("data")).isNull());
    }

    @Test
    void nestedObject() {
        List<String> lines = List.of("{\"db\": {\"host\": \"localhost\", \"port\": 5432}}");
        ConfigSection result = parser.parse(lines);

        ConfigNode db = result.children().get("db");
        assertInstanceOf(ConfigSection.class, db);
        ConfigSection dbSection = (ConfigSection) db;
        assertEquals("localhost", ((ConfigValue) dbSection.children().get("host")).asString().orElseThrow());
        assertEquals(5432, ((ConfigValue) dbSection.children().get("port")).asInt().orElseThrow());
    }

    @Test
    void arrayValue() {
        List<String> lines = List.of("{\"items\": [1, 2, 3]}");
        ConfigSection result = parser.parse(lines);

        ConfigNode items = result.children().get("items");
        assertInstanceOf(ConfigList.class, items);
        ConfigList list = (ConfigList) items;
        assertEquals(3, list.items().size());
        assertEquals(1, ((ConfigValue) list.items().get(0)).asInt().orElseThrow());
        assertEquals(2, ((ConfigValue) list.items().get(1)).asInt().orElseThrow());
        assertEquals(3, ((ConfigValue) list.items().get(2)).asInt().orElseThrow());
    }

    @Test
    void nestedArray() {
        List<String> lines = List.of("{\"matrix\": [[1, 2], [3, 4]]}");
        ConfigSection result = parser.parse(lines);

        ConfigNode matrix = result.children().get("matrix");
        assertInstanceOf(ConfigList.class, matrix);
    }

    @Test
    void floatValue() {
        List<String> lines = List.of("{\"pi\": 3.14}");
        ConfigSection result = parser.parse(lines);

        assertEquals(3.14, ((ConfigValue) result.children().get("pi")).asFloat().orElseThrow(), 0.001);
    }

    @Test
    void emptyInput() {
        ConfigSection result = parser.parse(List.of());
        assertTrue(result.children().isEmpty());
    }

    @Test
    void blankInput() {
        ConfigSection result = parser.parse(List.of("   "));
        assertTrue(result.children().isEmpty());
    }

    @Test
    void nullInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }

    @Test
    void multilineJson() {
        List<String> lines = List.of(
                "{",
                "  \"key\": \"value\"",
                "}"
        );
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
        assertEquals("value", ((ConfigValue) result.children().get("key")).asString().orElseThrow());
    }

    @Test
    void malformedJsonReturnsEmpty() {
        List<String> lines = List.of("{invalid json");
        ConfigSection result = parser.parse(lines);

        assertTrue(result.children().isEmpty());
    }
}
