package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IniParserTest {

    private final IniParser parser = new IniParser();

    @Test
    void globalKeyValue() {
        List<String> lines = List.of("key=value");
        ConfigSection result = parser.parse(lines);

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
        ConfigSection result = parser.parse(lines);

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
        ConfigSection result = parser.parse(lines);

        assertEquals(2, result.children().size());
        ConfigSection db = (ConfigSection) result.children().get("db");
        assertEquals("localhost", ((ConfigValue) db.children().get("host")).asString().orElseThrow());
        ConfigSection app = (ConfigSection) result.children().get("app");
        assertEquals("myapp", ((ConfigValue) app.children().get("name")).asString().orElseThrow());
    }

    @Test
    void semicolonCommentsIgnored() {
        List<String> lines = List.of(
                "; comment",
                "key=value"
        );
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
    }

    @Test
    void hashCommentsIgnored() {
        List<String> lines = List.of(
                "# comment",
                "key=value"
        );
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
    }

    @Test
    void emptyLinesSkipped() {
        List<String> lines = List.of(
                "",
                "[sec]",
                "key=value",
                ""
        );
        ConfigSection result = parser.parse(lines);

        ConfigSection sec = (ConfigSection) result.children().get("sec");
        assertEquals(1, sec.children().size());
    }

    @Test
    void spacesInSectionHeader() {
        List<String> lines = List.of(
                "[ database ]",
                "key=value"
        );
        ConfigSection result = parser.parse(lines);

        assertNotNull(result.children().get("database"));
    }

    @Test
    void emptySectionHeaderSkipped() {
        List<String> lines = List.of(
                "[]",
                "key=value"
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
    void globalKeysBeforeFirstSection() {
        List<String> lines = List.of(
                "version=1.0",
                "[app]",
                "name=test"
        );
        ConfigSection result = parser.parse(lines);

        assertNotNull(result.children().get("version"));
        assertNotNull(result.children().get("app"));
    }
}
