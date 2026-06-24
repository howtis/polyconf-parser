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
    void detectsYaml() {
        List<String> lines = List.of(
                "server:",
                "  host: localhost",
                "  port: 5432"
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
    void detectsKdl() {
        List<String> lines = List.of(
                "server {",
                "  host \"localhost\"",
                "  port 8080",
                "}"
        );
        assertEquals(Format.KDL, FormatClassifier.classify(lines));
    }

    @Test
    void ambiguousReturnsUnknown() {
        List<String> lines = List.of("key=value");
        assertEquals(Format.UNKNOWN, FormatClassifier.classify(lines));
    }
}
