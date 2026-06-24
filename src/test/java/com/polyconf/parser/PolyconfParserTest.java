package com.polyconf.parser;

import com.polyconf.parser.merge.MergePolicy;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolyconfParserTest {

    private final PolyconfParser parser = new PolyconfParser();

    @Test
    void singleTomlBlock() {
        List<String> lines = List.of(
                "[server]",
                "host = \"localhost\"",
                "port = 5432"
        );

        ParseResult result = parser.parse(lines);

        assertEquals(1, result.blocks().size());
        assertEquals(Format.TOML, result.blocks().get(0).detectedFormat());
        assertTrue(result.blocks().get(0).confidence() > 0.5);
        assertFalse(result.blocks().get(0).hinted());
        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings());
        assertEquals("localhost", result.flattened().get("server.host"));
    }

    @Test
    void singleJsonBlock() {
        List<String> lines = List.of(
                "{",
                "  \"name\": \"app\",",
                "  \"port\": 8080",
                "}"
        );

        ParseResult result = parser.parse(lines);

        assertEquals(1, result.blocks().size());
        assertEquals(Format.JSON, result.blocks().get(0).detectedFormat());
        assertEquals("app", result.flattened().get("name"));
    }

    @Test
    void singleYamlBlock() {
        List<String> lines = List.of(
                "server:",
                "  host: localhost",
                "  port: 8080"
        );

        ParseResult result = parser.parse(lines);

        assertEquals(1, result.blocks().size());
        assertEquals(Format.YAML, result.blocks().get(0).detectedFormat());
        assertEquals("localhost", result.flattened().get("server.host"));
    }

    @Test
    void mixedFormatWithHints() {
        List<String> lines = List.of(
                "# @fmt:toml",
                "[server]",
                "host = \"localhost\"",
                "port = 5432",
                "",
                "# @fmt:yaml",
                "database:",
                "  host: localhost",
                "  port: 5432"
        );

        ParseResult result = parser.parse(lines);

        assertEquals(2, result.blocks().size());

        assertEquals(Format.TOML, result.blocks().get(0).detectedFormat());
        assertTrue(result.blocks().get(0).hinted());
        assertEquals(1.0, result.blocks().get(0).confidence());

        assertEquals(Format.YAML, result.blocks().get(1).detectedFormat());
        assertTrue(result.blocks().get(1).hinted());

        assertEquals("localhost", result.flattened().get("server.host"));
        assertEquals("localhost", result.flattened().get("database.host"));
    }

    @Test
    void mixedFormatWithoutHints() {
        List<String> lines = List.of(
                "{",
                "  \"name\": \"app\"",
                "}",
                "",
                "server.host=localhost",
                "server.port=5432"
        );

        ParseResult result = parser.parse(lines);

        assertEquals(2, result.blocks().size());
        assertEquals(Format.JSON, result.blocks().get(0).detectedFormat());
        assertFalse(result.blocks().get(0).hinted());
        assertEquals(Format.PROPERTIES, result.blocks().get(1).detectedFormat());
        assertFalse(result.blocks().get(1).hinted());
    }

    @Test
    void hintOverridesClassifier() {
        List<String> lines = List.of(
                "# @fmt:json",
                "{\"key\": \"value\"}"
        );

        ParseResult result = parser.parse(lines);

        assertEquals(1, result.blocks().size());
        assertEquals(Format.JSON, result.blocks().get(0).detectedFormat());
        assertTrue(result.blocks().get(0).hinted());
        assertEquals("value", result.flattened().get("key"));
    }

    @Test
    void emptyInput() {
        ParseResult result = parser.parse(List.of());

        assertEquals(0, result.blocks().size());
        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings());
        assertTrue(result.flattened().isEmpty());
    }

    @Test
    void blankOnlyLines() {
        ParseResult result = parser.parse(List.of("", "   ", ""));

        assertEquals(0, result.blocks().size());
        assertFalse(result.hasErrors());
    }

    @Test
    void singlePropertiesBlock() {
        List<String> lines = List.of(
                "app.name=polyconf",
                "app.version=1.0"
        );

        ParseResult result = parser.parse(lines);

        assertEquals(1, result.blocks().size());
        assertEquals(Format.PROPERTIES, result.blocks().get(0).detectedFormat());
        assertEquals("polyconf", result.flattened().get("app.name"));
    }

    @Test
    void blockResultMetadata() {
        List<String> lines = List.of(
                "[section]",
                "key = \"value\""
        );

        ParseResult result = parser.parse(lines);

        assertEquals(1, result.blocks().size());
        assertEquals(0, result.blocks().get(0).startLine());
        assertEquals(1, result.blocks().get(0).endLine());
        assertEquals(Format.TOML, result.blocks().get(0).detectedFormat());
        assertFalse(result.blocks().get(0).hinted());
    }

    @Test
    void ambiguousBlockIdenticalResultsNoError() {
        List<String> lines = List.of(
                "key=value",
                "count=10"
        );

        ParseResult result = parser.parse(lines);

        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings());
        assertEquals(1, result.blocks().size());
        assertEquals("value", result.flattened().get("key"));
        assertEquals("10", result.flattened().get("count"));
    }

    @Test
    void hintedFormatsProduceMergeWithFallback() {
        List<String> lines = List.of(
                "# @fmt:toml",
                "port = 5432",
                "",
                "# @fmt:properties",
                "port=9090"
        );

        ParseResult result = new PolyconfParser().parse(lines);

        assertEquals(2, result.blocks().size());
        assertTrue(result.blocks().get(0).hinted());
        assertTrue(result.blocks().get(1).hinted());
        assertEquals("9090", result.flattened().get("port").toString());
    }

    @Test
    void nullLinesThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }

    @Test
    void nullPolicyThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of("key=value"), null));
    }

    @Test
    void mergePolicyOverwrite() {
        List<String> lines = List.of(
                "[server]",
                "port = 8080",
                "",
                "# @fmt:toml",
                "[server]",
                "port = 9090"
        );

        ParseResult result = parser.parse(lines);

        assertFalse(result.hasWarnings());
        assertEquals(9090L, result.flattened().get("server.port"));
    }

    @Test
    void flattenedReturnsAllKeys() {
        List<String> lines = List.of(
                "# @fmt:toml",
                "[app]",
                "name = \"test\"",
                "debug = true"
        );

        ParseResult result = parser.parse(lines);

        assertEquals("test", result.flattened().get("app.name"));
        assertEquals(true, result.flattened().get("app.debug"));
    }

    @Test
    void csvBlock() {
        List<String> lines = List.of(
                "name,age,city",
                "Alice,30,Seoul",
                "Bob,25,Busan"
        );

        ParseResult result = parser.parse(lines);

        assertEquals(1, result.blocks().size());
        assertEquals(Format.CSV, result.blocks().get(0).detectedFormat());
    }
}
