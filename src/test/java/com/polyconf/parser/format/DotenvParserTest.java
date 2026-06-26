package com.polyconf.parser.format;

import com.polyconf.parser.model.ConfigSection;
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
        assertEquals("localhost", result.childValue("DATABASE_HOST").orElseThrow().asString().orElseThrow());
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
        assertEquals("localhost", result.childValue("HOST").orElseThrow().asString().orElseThrow());
        assertEquals("5432", result.childValue("PORT").orElseThrow().asString().orElseThrow());
        assertEquals("true", result.childValue("DEBUG").orElseThrow().asString().orElseThrow());
    }

    @Test
    void exportPrefix() {
        List<String> lines = List.of("export DATABASE_URL=postgres://localhost/db");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("postgres://localhost/db", result.childValue("DATABASE_URL").orElseThrow().asString().orElseThrow());
    }

    @Test
    void doubleQuotedValue() {
        List<String> lines = List.of("NAME=\"John Doe\"");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("John Doe", result.childValue("NAME").orElseThrow().asString().orElseThrow());
    }

    @Test
    void singleQuotedValue() {
        List<String> lines = List.of("NAME='John Doe'");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("John Doe", result.childValue("NAME").orElseThrow().asString().orElseThrow());
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

        assertEquals("value", result.childValue("KEY").orElseThrow().asString().orElseThrow());
    }

    @Test
    void valueWithSpacesUnquoted() {
        List<String> lines = List.of("MESSAGE=hello world");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("hello world", result.childValue("MESSAGE").orElseThrow().asString().orElseThrow());
    }

    @Test
    void exportWithQuotedValue() {
        List<String> lines = List.of("export APP_NAME=\"My Application\"");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("My Application", result.childValue("APP_NAME").orElseThrow().asString().orElseThrow());
    }

    @Test
    void emptyValue() {
        List<String> lines = List.of("EMPTY=");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("", result.childValue("EMPTY").orElseThrow().asString().orElseThrow());
    }

    @Test
    void keyWithUnderscoresAndNumbers() {
        List<String> lines = List.of("MY_KEY_2=value");
        ConfigSection result = parser.parse(lines).section();

        assertEquals("value", result.childValue("MY_KEY_2").orElseThrow().asString().orElseThrow());
    }

    @Test
    void variableExpansion() {
        List<String> lines = List.of(
                "HOST=localhost",
                "URL=http://${HOST}:8080"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals("http://localhost:8080", result.childValue("URL").orElseThrow().asString().orElseThrow());
    }

    @Test
    void variableExpansionDollarSign() {
        List<String> lines = List.of(
                "HOST=localhost",
                "URL=http://$HOST:8080"
        );
        ConfigSection result = parser.parse(lines).section();

        assertEquals("http://localhost:8080", result.childValue("URL").orElseThrow().asString().orElseThrow());
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

        String cert = result.childValue("CERT").orElseThrow().asString().orElseThrow();
        assertTrue(cert.contains("line2"));
        assertTrue(cert.contains("BEGIN"));
    }




    @Test
    void selfReferencingVariableCircular() {
        List<String> lines = List.of("A=${A}");
        ParserResult pr = parser.parse(lines);
        assertTrue(pr.diagnostics().stream()
                .anyMatch(d -> d.level() == DiagnosticLevel.ERROR && d.message().contains("circular")));
    }
}
