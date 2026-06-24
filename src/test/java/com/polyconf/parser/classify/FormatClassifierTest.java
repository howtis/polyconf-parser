package com.polyconf.parser.classify;

import com.polyconf.parser.model.Format;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FormatClassifierTest {

    @Test
    void detectsJson() {
        List<String> lines = List.of(
                "{",
                "  \"name\": \"app\",",
                "  \"port\": 5432",
                "}"
        );
        assertEquals(Format.JSON, FormatClassifier.classify(lines));
    }

    @Test
    void detectsJsonArray() {
        List<String> lines = List.of(
                "[",
                "  {\"key\": \"value\"}",
                "]"
        );
        assertEquals(Format.JSON, FormatClassifier.classify(lines));
    }

    @Test
    void detectsYaml() {
        List<String> lines = List.of(
                "server:",
                "  host: localhost",
                "  port: 5432"
        );
        assertEquals(Format.YAML, FormatClassifier.classify(lines));
    }

    @Test
    void detectsYamlWithDocumentSeparator() {
        List<String> lines = List.of(
                "---",
                "name: hello"
        );
        assertEquals(Format.YAML, FormatClassifier.classify(lines));
    }

    @Test
    void detectsToml() {
        List<String> lines = List.of(
                "[server]",
                "host = \"localhost\"",
                "port = 5432"
        );
        assertEquals(Format.TOML, FormatClassifier.classify(lines));
    }

    @Test
    void detectsTomlArray() {
        List<String> lines = List.of(
                "[[servers]]",
                "host = \"a\"",
                "",
                "[[servers]]",
                "host = \"b\""
        );
        assertEquals(Format.TOML, FormatClassifier.classify(lines));
    }

    @Test
    void detectsIni() {
        List<String> lines = List.of(
                "[database]",
                "host=localhost",
                "port=5432"
        );
        assertEquals(Format.INI, FormatClassifier.classify(lines));
    }

    @Test
    void detectsProperties() {
        List<String> lines = List.of(
                "server.host=localhost",
                "server.port=5432"
        );
        assertEquals(Format.PROPERTIES, FormatClassifier.classify(lines));
    }

    @Test
    void detectsXml() {
        List<String> lines = List.of(
                "<server>",
                "  <host>localhost</host>",
                "  <port>5432</port>",
                "</server>"
        );
        assertEquals(Format.XML, FormatClassifier.classify(lines));
    }

    @Test
    void detectsDotenv() {
        List<String> lines = List.of(
                "DATABASE_HOST=localhost",
                "DATABASE_PORT=5432"
        );
        assertEquals(Format.DOTENV, FormatClassifier.classify(lines));
    }

    @Test
    void detectsCsv() {
        List<String> lines = List.of(
                "name,age,city",
                "Alice,30,Seoul",
                "Bob,25,Busan"
        );
        assertEquals(Format.CSV, FormatClassifier.classify(lines));
    }

    @Test
    void ambiguousReturnsUnknown() {
        List<String> lines = List.of("key=value");
        assertEquals(Format.UNKNOWN, FormatClassifier.classify(lines));
    }

    @Test
    void scoreMapReturnsScoresForTiedFormats() {
        var scores = FormatClassifier.scoreMap(List.of("key=value"));
        assertTrue(scores.containsKey(Format.INI));
        assertTrue(scores.containsKey(Format.PROPERTIES));
        assertEquals(scores.get(Format.INI), scores.get(Format.PROPERTIES));
    }

    @Test
    void scoreMapReturnsEmptyForAllBlank() {
        var scores = FormatClassifier.scoreMap(List.of("", "  "));
        assertTrue(scores.isEmpty());
    }

    @Test
    void emptyBlockReturnsUnknown() {
        assertEquals(Format.UNKNOWN, FormatClassifier.classify(List.of()));
    }

    @Test
    void commentOnlyBlockReturnsUnknown() {
        List<String> lines = List.of("# comment");
        assertEquals(Format.UNKNOWN, FormatClassifier.classify(lines));
    }

    @Test
    void propertiesWithColonSeparator() {
        List<String> lines = List.of(
                "server.host: localhost",
                "server.port: 5432"
        );
        assertEquals(Format.PROPERTIES, FormatClassifier.classify(lines));
    }

    @Test
    void dotenvWithExport() {
        List<String> lines = List.of(
                "export DB_HOST=localhost",
                "export DB_PORT=5432"
        );
        assertEquals(Format.DOTENV, FormatClassifier.classify(lines));
    }

    @Test
    void csvRequiresMultipleCommas() {
        List<String> lines = List.of("a,b");
        assertNotEquals(Format.CSV, FormatClassifier.classify(lines));
    }

    @Test
    void jsonStandaloneLiterals() {
        List<String> lines = List.of("true");
        assertEquals(Format.JSON, FormatClassifier.classify(lines));
    }
}
