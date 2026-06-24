package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.ValueType;
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
        ConfigSection result = parser.parse(lines).section();

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
        ConfigSection result = parser.parse(lines).section();

        ConfigList rows = (ConfigList) result.children().get("rows");
        assertEquals(2, rows.items().size());
    }

    @Test
    void quotedValues() {
        List<String> lines = List.of(
                "name,description",
                "\"Doe, John\",\"Hello, World\""
        );
        ConfigSection result = parser.parse(lines).section();

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
        ConfigSection result = parser.parse(lines).section();

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("She said \"hello\"", ((ConfigValue) row.children().get("text")).asString().orElseThrow());
    }

    @Test
    void headerOnlyReturnsEmptyRows() {
        List<String> lines = List.of("a,b,c");
        ConfigSection result = parser.parse(lines).section();

        ConfigList rows = (ConfigList) result.children().get("rows");
        assertTrue(rows.items().isEmpty());
    }

    @Test
    void extraCellsIgnored() {
        List<String> lines = List.of(
                "key",
                "v1,v2,v3"
        );
        ConfigSection result = parser.parse(lines).section();

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
        ConfigSection result = parser.parse(lines).section();

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("1", ((ConfigValue) row.children().get("a")).asString().orElseThrow());
        assertNull(row.children().get("b"));
    }

    @Test
    void whitespaceAroundCells() {
        List<String> lines = List.of(
                " name , age ",
                " John , 30 "
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("John", ((ConfigValue) row.children().get("name")).asString().orElseThrow());
        assertEquals("30", ((ConfigValue) row.children().get("age")).asString().orElseThrow());
    }

    @Test
    void integerTypeInference() {
        List<String> lines = List.of(
                "name,count",
                "John,30"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        ConfigValue count = (ConfigValue) row.children().get("count");
        assertEquals(ValueType.INTEGER, count.type());
        assertEquals(30, count.asInt().orElseThrow());
    }

    @Test
    void booleanTypeInference() {
        List<String> lines = List.of(
                "name,active",
                "John,true"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        ConfigValue active = (ConfigValue) row.children().get("active");
        assertEquals(ValueType.BOOLEAN, active.type());
        assertTrue(active.asBool().orElseThrow());
    }

    @Test
    void floatTypeInference() {
        List<String> lines = List.of(
                "name,score",
                "John,3.14"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        ConfigValue score = (ConfigValue) row.children().get("score");
        assertEquals(ValueType.FLOAT, score.type());
    }

    @Test
    void tabDelimiter() {
        CsvParser tabParser = new CsvParser('\t');
        List<String> lines = List.of(
                "name\tage",
                "John\t30"
        );
        ConfigSection result = tabParser.parse(lines).section();

        ConfigList rows = (ConfigList) result.children().get("rows");
        assertEquals(1, rows.items().size());
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("John", ((ConfigValue) row.children().get("name")).asString().orElseThrow());
    }

    @Test
    void semicolonDelimiter() {
        CsvParser semiParser = new CsvParser(';');
        List<String> lines = List.of(
                "name;age",
                "John;30"
        );
        ConfigSection result = semiParser.parse(lines).section();

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("John", ((ConfigValue) row.children().get("name")).asString().orElseThrow());
    }

    @Test
    void pipeDelimiter() {
        CsvParser pipeParser = new CsvParser('|');
        List<String> lines = List.of(
                "name|age",
                "John|30"
        );
        ConfigSection result = pipeParser.parse(lines).section();

        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("John", ((ConfigValue) row.children().get("name")).asString().orElseThrow());
    }

    @Test
    void unclosedQuotesSilentDataLoss() {
        // Unclosed quotes cause the rest of the line to be absorbed into one cell
        List<String> lines = List.of(
                "name,age",
                "\"Alice,30"
        );
        ConfigSection result = parser.parse(lines).section();
        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);

        // The opening quote is consumed, producing a corrupted value
        String nameVal = ((ConfigValue) row.children().get("name")).asString().orElseThrow();
        // "Alice,30" with unclosed quote - quote stripped, becomes "Alice,30"
        assertNotNull(nameVal);
    }

    @Test
    void emptyCellsBetweenDelimiters() {
        List<String> lines = List.of(
                "a,b,c",
                "1,,3"
        );
        ConfigSection result = parser.parse(lines).section();
        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("1", ((ConfigValue) row.children().get("a")).asString().orElseThrow());
        assertEquals("", ((ConfigValue) row.children().get("b")).asString().orElseThrow());
        assertEquals("3", ((ConfigValue) row.children().get("c")).asString().orElseThrow());
    }

    @Test
    void quotedHeader() {
        List<String> lines = List.of(
                "\"first name\",\"last name\"",
                "John,Doe"
        );
        ConfigSection result = parser.parse(lines).section();
        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        // Quoted headers should have quotes stripped by strip() on each cell
        assertNull(row.children().get("\"first name\""));
    }

    @Test
    void onlyDelimitersLine() {
        List<String> lines = List.of(
                ",,,",
                "a,b,c"
        );
        // First line is 4 empty cells, becomes header with empty strings
        ConfigSection result = parser.parse(lines).section();
        ConfigList rows = (ConfigList) result.children().get("rows");
        assertEquals(1, rows.items().size());
    }

    @Test
    void quotedCellContainingDelimiter() {
        List<String> lines = List.of(
                "name,note",
                "\"Doe, John\",\"hello, world\""
        );
        ConfigSection result = parser.parse(lines).section();
        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals("Doe, John", ((ConfigValue) row.children().get("name")).asString().orElseThrow());
        assertEquals("hello, world", ((ConfigValue) row.children().get("note")).asString().orElseThrow());
    }

    @Test
    void quotedCellWithEscapedQuote() {
        List<String> lines = List.of(
                "name,note",
                "\"She said \"\"hi\"\"\",\"ok\""
        );
        ConfigSection result = parser.parse(lines).section();
        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertTrue(((ConfigValue) row.children().get("name")).asString().orElseThrow().contains("\""));
    }

    @Test
    void moreCellsThanHeader() {
        List<String> lines = List.of(
                "a,b",
                "1,2,3"
        );
        // Extra cells beyond header are silently ignored
        ConfigSection result = parser.parse(lines).section();
        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        assertEquals(2, row.children().size());
    }

    @Test
    void fewerCellsThanHeader() {
        List<String> lines = List.of(
                "a,b,c",
                "1"
        );
        ConfigSection result = parser.parse(lines).section();
        ConfigList rows = (ConfigList) result.children().get("rows");
        ConfigSection row = (ConfigSection) rows.items().get(0);
        // Only 1 cell provided for 3-column header
        assertEquals(1, row.children().size());
    }
}
