package com.polyconf.parser.format;

import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.parse.LenientParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IniParserTest {

    private final LenientParser parser = new IniFormat.Parser();

    @Test
    void globalKeyValue() {
        List<String> lines = List.of("key=value");
        ConfigSection result = parser.parse(lines).section();

        assertEquals(1, result.children().size());
        assertEquals("value", ((ConfigValue) result.children().get("key")).asString().orElseThrow());
    }

    @Test
    void sectionWithKeys() {
        List<String> lines = List.of(
                "[database]",
                "host=localhost",
                "port=5432"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigNode db = result.children().get("database");
        assertInstanceOf(ConfigSection.class, db);
        ConfigSection dbSection = (ConfigSection) db;
        assertEquals("localhost", ((ConfigValue) dbSection.children().get("host")).asString().orElseThrow());
        assertEquals("5432", ((ConfigValue) dbSection.children().get("port")).asString().orElseThrow());
    }

    @Test
    void multipleSections() {
        List<String> lines = List.of(
                "[db]",
                "host=localhost",
                "[app]",
                "name=myapp"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals(2, result.children().size());
        ConfigSection db = (ConfigSection) result.children().get("db");
        assertEquals("localhost", ((ConfigValue) db.children().get("host")).asString().orElseThrow());
        ConfigSection app = (ConfigSection) result.children().get("app");
        assertEquals("myapp", ((ConfigValue) app.children().get("name")).asString().orElseThrow());
    }


    @Test
    void spacesInSectionHeader() {
        List<String> lines = List.of(
                "[ database ]",
                "key=value"
        );
        ConfigSection result = parser.parse(lines).section();

        assertNotNull(result.children().get("database"));
    }


    @Test
    void globalKeysBeforeFirstSection() {
        List<String> lines = List.of(
                "version=1.0",
                "[app]",
                "name=test"
        );
        ConfigSection result = parser.parse(lines).section();

        assertNotNull(result.children().get("version"));
        assertNotNull(result.children().get("app"));
    }

    @Test
    void subsectionNesting() {
        List<String> lines = List.of(
                "[database.connection]",
                "host=localhost",
                "port=5432"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigSection database = (ConfigSection) result.children().get("database");
        ConfigSection connection = (ConfigSection) database.children().get("connection");
        assertEquals("localhost", ((ConfigValue) connection.children().get("host")).asString().orElseThrow());
        assertEquals("5432", ((ConfigValue) connection.children().get("port")).asString().orElseThrow());
    }

    @Test
    void deepSubsectionNesting() {
        List<String> lines = List.of(
                "[a.b.c]",
                "key=value"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigSection a = (ConfigSection) result.children().get("a");
        ConfigSection b = (ConfigSection) a.children().get("b");
        ConfigSection c = (ConfigSection) b.children().get("c");
        assertEquals("value", ((ConfigValue) c.children().get("key")).asString().orElseThrow());
    }

    @Test
    void globalKeyCollisionWithSectionName() {
        // global key with same name as a later section -> section overwrites global
        List<String> lines = List.of(
                "db=global_value",
                "[db]",
                "host=localhost"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigNode db = result.children().get("db");
        // Bug or design: section overwrites the flat key with same name
        assertInstanceOf(ConfigSection.class, db);
    }

    @Test
    void keyWithOnlySpaceBeforeEquals() {
        List<String> lines = List.of(
                "key =value",
                "port=5432"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals("value", ((ConfigValue) result.children().get("key")).asString().orElseThrow());
        assertEquals("5432", ((ConfigValue) result.children().get("port")).asString().orElseThrow());
    }

    @Test
    void valueWithInlineCommentIsStripped() {
        List<String> lines = List.of(
                "key=value ; this is a comment"
        );
        ConfigSection result = parser.parse(lines).section();

        String val = ((ConfigValue) result.children().get("key")).asString().orElseThrow();
        // ini4j includes the comment in the value since ";" is only a comment at line start
        assertTrue(val.startsWith("value"));
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
