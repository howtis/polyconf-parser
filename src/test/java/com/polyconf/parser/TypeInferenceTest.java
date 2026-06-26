package com.polyconf.parser;

import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TypeInferenceTest {

    private final PolyconfParser parser = new PolyconfParser();

    @Test
    void integerValueInToml() {
        ParseResult result = parser.parse("# @fmt:toml\nport = 5432\n");
        Map<String, Object> flat = result.flattened();

        assertEquals("5432", flat.get("port"));
    }

    @Test
    void integerValueInYaml() {
        ParseResult result = parser.parse("# @fmt:yaml\nport: 5432\n");
        Map<String, Object> flat = result.flattened();

        assertEquals("5432", flat.get("port"));
    }

    @Test
    void integerValueInJson() {
        ParseResult result = parser.parse("# @fmt:json\n{\"port\": 5432}\n");
        Map<String, Object> flat = result.flattened();

        assertEquals("5432", flat.get("port"));
    }

    @Test
    void booleanValueInToml() {
        ParseResult result = parser.parse("# @fmt:toml\nenabled = true\ndebug = false\n");
        Map<String, Object> flat = result.flattened();

        assertEquals("true", flat.get("enabled"));
        assertEquals("false", flat.get("debug"));
    }

    @Test
    void booleanValueInYaml() {
        ParseResult result = parser.parse("# @fmt:yaml\nenabled: true\ndebug: false\n");
        Map<String, Object> flat = result.flattened();

        assertEquals("true", flat.get("enabled"));
        assertEquals("false", flat.get("debug"));
    }

    @Test
    void booleanValueInJson() {
        ParseResult result = parser.parse("# @fmt:json\n{\"enabled\": true, \"debug\": false}\n");
        Map<String, Object> flat = result.flattened();

        assertEquals("true", flat.get("enabled"));
        assertEquals("false", flat.get("debug"));
    }

    @Test
    void floatValueInToml() {
        ParseResult result = parser.parse("# @fmt:toml\npi = 3.14\n");
        Map<String, Object> flat = result.flattened();

        assertInstanceOf(Double.class, flat.get("pi"));
        assertEquals(3.14, (Double) flat.get("pi"), 0.001);
    }

    @Test
    void floatValueInYaml() {
        ParseResult result = parser.parse("# @fmt:yaml\npi: 3.14\n");
        Map<String, Object> flat = result.flattened();

        assertInstanceOf(Double.class, flat.get("pi"));
        assertEquals(3.14, (Double) flat.get("pi"), 0.001);
    }

    @Test
    void floatValueInJson() {
        ParseResult result = parser.parse("# @fmt:json\n{\"pi\": 3.14}\n");
        Map<String, Object> flat = result.flattened();

        assertEquals("3.14", flat.get("pi"));
    }

    @Test
    void nullValueInYaml() {
        ParseResult result = parser.parse("# @fmt:yaml\noptional:\n");
        Map<String, Object> flat = result.flattened();

        assertNull(flat.get("optional"));
    }

    @Test
    void nullValueInJson() {
        ParseResult result = parser.parse("# @fmt:json\n{\"optional\": null}\n");
        Map<String, Object> flat = result.flattened();

        assertNull(flat.get("optional"));
    }
}
