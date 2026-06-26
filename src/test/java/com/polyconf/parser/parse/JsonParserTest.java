package com.polyconf.parser.parse;

import com.polyconf.parser.format.JsonFormat;
import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
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
        assertEquals("hello", ((ConfigValue) result.children().get("name")).asString().orElseThrow());
        assertEquals(42, ((ConfigValue) result.children().get("value")).asInt().orElseThrow());
    }

    @Test
    void booleanAndNull() {
        List<String> lines = List.of("{\"active\": true, \"data\": null}");
        ConfigSection result = parser.parse(lines).section();

        assertTrue(((ConfigValue) result.children().get("active")).asBool().orElseThrow());
        assertTrue(((ConfigValue) result.children().get("data")).isNull());
    }

    @Test
    void nestedObject() {
        List<String> lines = List.of("{\"db\": {\"host\": \"localhost\", \"port\": 5432}}");
        ConfigSection result = parser.parse(lines).section();

        ConfigNode db = result.children().get("db");
        assertInstanceOf(ConfigSection.class, db);
        ConfigSection dbSection = (ConfigSection) db;
        assertEquals("localhost", ((ConfigValue) dbSection.children().get("host")).asString().orElseThrow());
        assertEquals(5432, ((ConfigValue) dbSection.children().get("port")).asInt().orElseThrow());
    }

    @Test
    void arrayValue() {
        List<String> lines = List.of("{\"items\": [1, 2, 3]}");
        ConfigSection result = parser.parse(lines).section();

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
        ConfigSection result = parser.parse(lines).section();

        ConfigNode matrix = result.children().get("matrix");
        assertInstanceOf(ConfigList.class, matrix);
    }

    @Test
    void floatValue() {
        List<String> lines = List.of("{\"pi\": 3.14}");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(3.14, ((ConfigValue) result.children().get("pi")).asFloat().orElseThrow(), 0.001);
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
        assertEquals("value", ((ConfigValue) result.children().get("key")).asString().orElseThrow());
    }

    @Test
    void rootArray() {
        List<String> lines = List.of("[1, 2, 3]");
        ParserResult pr = parser.parse(lines);

        ConfigList root = (ConfigList) pr.section().children().get("root");
        assertEquals(3, root.items().size());
        assertEquals(1, ((ConfigValue) root.items().get(0)).asInt().orElseThrow());
        assertEquals(2, ((ConfigValue) root.items().get(1)).asInt().orElseThrow());
        assertEquals(3, ((ConfigValue) root.items().get(2)).asInt().orElseThrow());
    }

    @Test
    void rootArrayOfObjects() {
        List<String> lines = List.of("[{\"name\": \"a\"}, {\"name\": \"b\"}]");
        ParserResult pr = parser.parse(lines);

        ConfigList root = (ConfigList) pr.section().children().get("root");
        assertEquals(2, root.items().size());
        ConfigSection first = (ConfigSection) root.items().get(0);
        assertEquals("a", ((ConfigValue) first.children().get("name")).asString().orElseThrow());
    }

}
