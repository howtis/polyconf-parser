package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DotenvParserTest {

    private final DotenvParser parser = new DotenvParser();

    @Test
    void basicKeyValue() {
        List<String> lines = List.of("DATABASE_HOST=localhost");
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
        ConfigNode node = result.children().get("DATABASE_HOST");
        assertInstanceOf(ConfigValue.class, node);
        assertEquals("localhost", ((ConfigValue) node).asString().orElseThrow());
    }

    @Test
    void multipleEntries() {
        List<String> lines = List.of(
                "HOST=localhost",
                "PORT=5432",
                "DEBUG=true"
        );
        ConfigSection result = parser.parse(lines);

        assertEquals(3, result.children().size());
        assertEquals("localhost", ((ConfigValue) result.children().get("HOST")).asString().orElseThrow());
        assertEquals("5432", ((ConfigValue) result.children().get("PORT")).asString().orElseThrow());
        assertEquals("true", ((ConfigValue) result.children().get("DEBUG")).asString().orElseThrow());
    }

    @Test
    void exportPrefix() {
        List<String> lines = List.of("export DATABASE_URL=postgres://localhost/db");
        ConfigSection result = parser.parse(lines);

        ConfigNode node = result.children().get("DATABASE_URL");
        assertNotNull(node);
        assertEquals("postgres://localhost/db", ((ConfigValue) node).asString().orElseThrow());
    }

    @Test
    void doubleQuotedValue() {
        List<String> lines = List.of("NAME=\"John Doe\"");
        ConfigSection result = parser.parse(lines);

        assertEquals("John Doe", ((ConfigValue) result.children().get("NAME")).asString().orElseThrow());
    }

    @Test
    void singleQuotedValue() {
        List<String> lines = List.of("NAME='John Doe'");
        ConfigSection result = parser.parse(lines);

        assertEquals("John Doe", ((ConfigValue) result.children().get("NAME")).asString().orElseThrow());
    }

    @Test
    void commentsIgnored() {
        List<String> lines = List.of(
                "# Database config",
                "HOST=localhost"
        );
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
        assertEquals("localhost", ((ConfigValue) result.children().get("HOST")).asString().orElseThrow());
    }

    @Test
    void emptyLinesSkipped() {
        List<String> lines = List.of(
                "",
                "KEY=value",
                "",
                ""
        );
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
    }

    @Test
    void lineWithoutEqualsSkipped() {
        List<String> lines = List.of(
                "invalid line",
                "KEY=value"
        );
        ConfigSection result = parser.parse(lines);

        assertEquals(1, result.children().size());
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
    void spacesAroundEquals() {
        List<String> lines = List.of("KEY = value");
        ConfigSection result = parser.parse(lines);

        assertEquals("value", ((ConfigValue) result.children().get("KEY")).asString().orElseThrow());
    }

    @Test
    void valueWithSpacesUnquoted() {
        List<String> lines = List.of("MESSAGE=hello world");
        ConfigSection result = parser.parse(lines);

        assertEquals("hello world", ((ConfigValue) result.children().get("MESSAGE")).asString().orElseThrow());
    }

    @Test
    void exportWithQuotedValue() {
        List<String> lines = List.of("export APP_NAME=\"My Application\"");
        ConfigSection result = parser.parse(lines);

        assertEquals("My Application", ((ConfigValue) result.children().get("APP_NAME")).asString().orElseThrow());
    }

    @Test
    void emptyValue() {
        List<String> lines = List.of("EMPTY=");
        ConfigSection result = parser.parse(lines);

        assertEquals("", ((ConfigValue) result.children().get("EMPTY")).asString().orElseThrow());
    }

    @Test
    void keyWithUnderscoresAndNumbers() {
        List<String> lines = List.of("MY_KEY_2=value");
        ConfigSection result = parser.parse(lines);

        assertEquals("value", ((ConfigValue) result.children().get("MY_KEY_2")).asString().orElseThrow());
    }
}
