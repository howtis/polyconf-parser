package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserTest {

    private final CsvParser parser = new CsvParser();

    @Test
    void basicHeaderAndRow() {
        List<String> lines = List.of(
                "name,age,city",
                "John,30,Seoul"
        );
        ConfigSection result = parser.parse(lines);

        ConfigList rows = (ConfigList) result.children().get("rows");
        assertEquals(1, rows.items().size());
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("John", ((ConfigValue) row.children().get("name")).asString().orElseThrow());
        assertEquals("30", ((ConfigValue) row.children().get("age")).asString().orElseThrow());
        assertEquals("Seoul", ((ConfigValue) row.children().get("city")).asString().orElseThrow());
    }

    @Test
    void multipleRows() {
        List<String> lines = List.of(
                "name,age",
                "Alice,25",
                "Bob,35"
        );
        ConfigSection result = parser.parse(lines);

        ConfigList rows = (ConfigList) result.children().get("rows");
        assertEquals(2, rows.items().size());
    }

    @Test
    void quotedValues() {
        List<String> lines = List.of(
                "name,description",
                "\"Doe, John\",\"Hello, World\""
        );
        ConfigSection result = parser.parse(lines);

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("Doe, John", ((ConfigValue) row.children().get("name")).asString().orElseThrow());
        assertEquals("Hello, World", ((ConfigValue) row.children().get("description")).asString().orElseThrow());
    }

    @Test
    void escapedQuotes() {
        List<String> lines = List.of(
                "text",
                "\"She said \"\"hello\"\"\""
        );
        ConfigSection result = parser.parse(lines);

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("She said \"hello\"", ((ConfigValue) row.children().get("text")).asString().orElseThrow());
    }

    @Test
    void emptyLinesSkipped() {
        List<String> lines = List.of(
                "key",
                "",
                "value"
        );
        ConfigSection result = parser.parse(lines);

        ConfigList rows = (ConfigList) result.children().get("rows");
        assertEquals(1, rows.items().size());
    }

    @Test
    void headerOnlyReturnsEmptyRows() {
        List<String> lines = List.of("a,b,c");
        ConfigSection result = parser.parse(lines);

        ConfigList rows = (ConfigList) result.children().get("rows");
        assertTrue(rows.items().isEmpty());
    }

    @Test
    void extraCellsIgnored() {
        List<String> lines = List.of(
                "key",
                "v1,v2,v3"
        );
        ConfigSection result = parser.parse(lines);

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertNull(row.children().get("v2"));
    }

    @Test
    void missingCellsEmpty() {
        List<String> lines = List.of(
                "a,b,c",
                "1"
        );
        ConfigSection result = parser.parse(lines);

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("1", ((ConfigValue) row.children().get("a")).asString().orElseThrow());
        assertNull(row.children().get("b"));
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
    void whitespaceAroundCells() {
        List<String> lines = List.of(
                " name , age ",
                " John , 30 "
        );
        ConfigSection result = parser.parse(lines);

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("John", ((ConfigValue) row.children().get("name")).asString().orElseThrow());
        assertEquals("30", ((ConfigValue) row.children().get("age")).asString().orElseThrow());
    }
}
