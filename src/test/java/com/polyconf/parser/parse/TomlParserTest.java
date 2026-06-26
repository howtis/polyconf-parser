package com.polyconf.parser.parse;

import com.polyconf.parser.format.TomlFormat;
import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.ValueType;
import com.polyconf.parser.parse.LenientParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TomlParserTest {

    private final LenientParser parser = new TomlFormat.Parser();

    @Test
    void basicKeyValue() {
        List<String> lines = List.of("name = \"hello\"");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(1, result.children().size());
        assertEquals("hello", ((ConfigValue) result.children().get("name")).asString().orElseThrow());
    }

    @Test
    void integerValue() {
        List<String> lines = List.of("port = 5432");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(5432, ((ConfigValue) result.children().get("port")).asInt().orElseThrow());
    }

    @Test
    void booleanValues() {
        List<String> lines = List.of("enabled = true", "debug = false");
        ConfigSection result = parser.parse(lines).section();

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
        ConfigSection result = parser.parse(lines).section();

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
        ConfigSection result = parser.parse(lines).section();

        ConfigSection database = (ConfigSection) result.children().get("database");
        ConfigSection connection = (ConfigSection) database.children().get("connection");
        assertEquals("localhost", ((ConfigValue) connection.children().get("host")).asString().orElseThrow());
    }

    @Test
    void arrayValue() {
        List<String> lines = List.of("ports = [8080, 8081, 8082]");
        ConfigSection result = parser.parse(lines).section();

        ConfigList ports = (ConfigList) result.children().get("ports");
        assertEquals(3, ports.items().size());
    }

    @Test
    void floatValue() {
        List<String> lines = List.of("pi = 3.14");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(3.14, ((ConfigValue) result.children().get("pi")).asFloat().orElseThrow(), 0.001);
    }

    @Test
    void dottedKey() {
        List<String> lines = List.of("database.host = \"localhost\"");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection database = (ConfigSection) result.children().get("database");
        assertEquals("localhost", ((ConfigValue) database.children().get("host")).asString().orElseThrow());
    }

    @Test
    void localDateValue() {
        List<String> lines = List.of("birthday = 2023-12-25");
        ParserResult pr = parser.parse(lines);

        ConfigValue val = (ConfigValue) pr.section().children().get("birthday");
        assertEquals(ValueType.DATE, val.type());
        assertInstanceOf(java.time.LocalDate.class, val.rawValue());
    }

    @Test
    void localDateTimeValue() {
        List<String> lines = List.of("created = 2023-12-25T10:30:00");
        ParserResult pr = parser.parse(lines);

        ConfigValue val = (ConfigValue) pr.section().children().get("created");
        assertEquals(ValueType.DATETIME, val.type());
    }

    @Test
    void offsetDateTimeValue() {
        List<String> lines = List.of("timestamp = 2023-12-25T10:30:00+09:00");
        ParserResult pr = parser.parse(lines);

        ConfigValue val = (ConfigValue) pr.section().children().get("timestamp");
        assertEquals(ValueType.DATETIME, val.type());
    }

    @Test
    void arrayOfTables() {
        List<String> lines = List.of(
                "[[servers]]",
                "host = \"a\"",
                "port = 1",
                "",
                "[[servers]]",
                "host = \"b\"",
                "port = 2"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigList servers = (ConfigList) result.children().get("servers");
        assertEquals(2, servers.items().size());
        ConfigSection first = (ConfigSection) servers.items().get(0);
        assertEquals("a", ((ConfigValue) first.children().get("host")).asString().orElseThrow());
    }

    @Test
    void inlineTable() {
        List<String> lines = List.of("db = { host = \"localhost\", port = 5432 }");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection db = (ConfigSection) result.children().get("db");
        assertEquals("localhost", ((ConfigValue) db.children().get("host")).asString().orElseThrow());
        assertEquals(5432, ((ConfigValue) db.children().get("port")).asInt().orElseThrow());
    }
}
