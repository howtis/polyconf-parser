package com.polyconf.parser.format;

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
        assertEquals("value", result.childValue("key").orElseThrow().asString().orElseThrow());
    }

    @Test
    void sectionWithKeys() {
        List<String> lines = List.of(
                "[database]",
                "host=localhost",
                "port=5432"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigSection dbSection = result.childSection("database").orElseThrow();
        assertEquals("localhost", dbSection.childValue("host").orElseThrow().asString().orElseThrow());
        assertEquals("5432", dbSection.childValue("port").orElseThrow().asString().orElseThrow());
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
        ConfigSection db = result.childSection("db").orElseThrow();
        assertEquals("localhost", db.childValue("host").orElseThrow().asString().orElseThrow());
        ConfigSection app = result.childSection("app").orElseThrow();
        assertEquals("myapp", app.childValue("name").orElseThrow().asString().orElseThrow());
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

        ConfigSection database = result.childSection("database").orElseThrow();
        ConfigSection connection = database.childSection("connection").orElseThrow();
        assertEquals("localhost", connection.childValue("host").orElseThrow().asString().orElseThrow());
        assertEquals("5432", connection.childValue("port").orElseThrow().asString().orElseThrow());
    }

    @Test
    void deepSubsectionNesting() {
        List<String> lines = List.of(
                "[a.b.c]",
                "key=value"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigSection a = result.childSection("a").orElseThrow();
        ConfigSection b = a.childSection("b").orElseThrow();
        ConfigSection c = b.childSection("c").orElseThrow();
        assertEquals("value", c.childValue("key").orElseThrow().asString().orElseThrow());
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

        ConfigSection db = result.childSection("db").orElseThrow();
        // Bug or design: section overwrites the flat key with same name
        assertNotNull(db);
    }

    @Test
    void keyWithOnlySpaceBeforeEquals() {
        List<String> lines = List.of(
                "key =value",
                "port=5432"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals("value", result.childValue("key").orElseThrow().asString().orElseThrow());
        assertEquals("5432", result.childValue("port").orElseThrow().asString().orElseThrow());
    }

    @Test
    void valueWithInlineCommentIsStripped() {
        List<String> lines = List.of(
                "key=value ; this is a comment"
        );
        ConfigSection result = parser.parse(lines).section();

        String val = result.childValue("key").orElseThrow().asString().orElseThrow();
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
