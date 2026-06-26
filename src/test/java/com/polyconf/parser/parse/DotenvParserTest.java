package com.polyconf.parser.parse;

import com.polyconf.parser.format.DotenvFormat;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.parse.LenientParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DotenvParserTest {

    private final LenientParser parser = new DotenvFormat.Parser();

    @Test
    void basicKeyValue() {
        List<String> lines = List.of("DATABASE_HOST=localhost");
        ConfigSection result = parser.parse(lines).section();

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
        ConfigSection result = parser.parse(lines).section();

        assertEquals(3, result.children().size());
        assertEquals("localhost", ((ConfigValue) result.children().get("HOST")).asString().orElseThrow());
        assertEquals("5432", ((ConfigValue) result.children().get("PORT")).asString().orElseThrow());
        assertEquals("true", ((ConfigValue) result.children().get("DEBUG")).asString().orElseThrow());
    }

    @Test
    void exportPrefix() {
        List<String> lines = List.of("export DATABASE_URL=postgres://localhost/db");
        ConfigSection result = parser.parse(lines).section();

        ConfigNode node = result.children().get("DATABASE_URL");
        assertNotNull(node);
        assertEquals("postgres://localhost/db", ((ConfigValue) node).asString().orElseThrow());
    }

    @Test
    void doubleQuotedValue() {
        List<String> lines = List.of("NAME=\"John Doe\"");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("John Doe", ((ConfigValue) result.children().get("NAME")).asString().orElseThrow());
    }

    @Test
    void singleQuotedValue() {
        List<String> lines = List.of("NAME='John Doe'");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("John Doe", ((ConfigValue) result.children().get("NAME")).asString().orElseThrow());
    }



    @Test
    void lineWithoutEqualsSkipped() {
        List<String> lines = List.of(
                "invalid line",
                "KEY=value"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals(1, result.children().size());
    }


    @Test
    void spacesAroundEquals() {
        List<String> lines = List.of("KEY = value");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("value", ((ConfigValue) result.children().get("KEY")).asString().orElseThrow());
    }

    @Test
    void valueWithSpacesUnquoted() {
        List<String> lines = List.of("MESSAGE=hello world");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("hello world", ((ConfigValue) result.children().get("MESSAGE")).asString().orElseThrow());
    }

    @Test
    void exportWithQuotedValue() {
        List<String> lines = List.of("export APP_NAME=\"My Application\"");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("My Application", ((ConfigValue) result.children().get("APP_NAME")).asString().orElseThrow());
    }

    @Test
    void emptyValue() {
        List<String> lines = List.of("EMPTY=");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("", ((ConfigValue) result.children().get("EMPTY")).asString().orElseThrow());
    }

    @Test
    void keyWithUnderscoresAndNumbers() {
        List<String> lines = List.of("MY_KEY_2=value");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("value", ((ConfigValue) result.children().get("MY_KEY_2")).asString().orElseThrow());
    }

    @Test
    void variableExpansion() {
        List<String> lines = List.of(
                "HOST=localhost",
                "URL=http://${HOST}:8080"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals("http://localhost:8080", ((ConfigValue) result.children().get("URL")).asString().orElseThrow());
    }

    @Test
    void variableExpansionDollarSign() {
        List<String> lines = List.of(
                "HOST=localhost",
                "URL=http://$HOST:8080"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals("http://localhost:8080", ((ConfigValue) result.children().get("URL")).asString().orElseThrow());
    }

    @Test
    void undefinedVariableWarning() {
        List<String> lines = List.of("URL=${UNDEFINED}");
        ParserResult pr = parser.parse(lines);

        assertTrue(pr.diagnostics().stream()
                .anyMatch(d -> d.level() == DiagnosticLevel.WARNING && d.message().contains("UNDEFINED")));
    }

    @Test
    void circularVariableError() {
        List<String> lines = List.of(
                "A=${B}",
                "B=${A}"
        );
        ParserResult pr = parser.parse(lines);

        assertTrue(pr.diagnostics().stream()
                .anyMatch(d -> d.level() == DiagnosticLevel.ERROR && d.message().contains("circular")));
    }

    @Test
    void multilineQuotedValue() {
        List<String> lines = List.of(
                "CERT=\"-----BEGIN",
                "line2",
                "line3-----\""
        );
        ConfigSection result = parser.parse(lines).section();

        assertTrue(((ConfigValue) result.children().get("CERT")).asString().orElseThrow().contains("line2"));
        assertTrue(((ConfigValue) result.children().get("CERT")).asString().orElseThrow().contains("BEGIN"));
    }




    @Test
    void selfReferencingVariableCircular() {
        List<String> lines = List.of("A=${A}");
        ParserResult pr = parser.parse(lines);
        assertTrue(pr.diagnostics().stream()
                .anyMatch(d -> d.level() == DiagnosticLevel.ERROR && d.message().contains("circular")));
    }
}
