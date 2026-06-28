package com.polyconf.parser;

import com.polyconf.parser.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StructuredFallbackTest {

    /**
     * When all parsers fail and the fallback is reached,
     * indentation-based structure should be recovered
     * instead of flattened into key=value pairs.
     */
    @Test
    void recoversYamlLikeIndentationStructure() {
        List<String> lines = List.of(
                "database:",
                "  host: localhost",
                "  port: 5432"
        );

        ParseResult result = new PolyconfParser().parse(lines);
        ConfigSection root = result.root();
        Map<String, Object> flat = result.flattened();

        // database should be a section, not a flat key with empty value
        ConfigSection dbSection = root.childSection("database").orElse(null);
        assertNotNull(dbSection, "database should be a nested section");

        assertEquals("localhost", dbSection.childValue("host").orElseThrow().rawValue());
        assertEquals("5432", dbSection.childValue("port").orElseThrow().rawValue());

        // Flattened access should work via dotted path
        assertEquals("localhost", flat.get("database.host"));
        assertEquals("5432", flat.get("database.port"));
    }

    @Test
    void recoversMultiSectionIndentation() {
        List<String> lines = List.of(
                "server:",
                "  host: 0.0.0.0",
                "  port: 8080",
                "database:",
                "  host: localhost",
                "  port: 5432"
        );

        ParseResult result = new PolyconfParser().parse(lines);
        ConfigSection root = result.root();

        assertTrue(root.childSection("server").isPresent());
        assertTrue(root.childSection("database").isPresent());

        assertEquals("0.0.0.0", root.childSection("server").orElseThrow()
                .childValue("host").orElseThrow().rawValue());
        assertEquals("localhost", root.childSection("database").orElseThrow()
                .childValue("host").orElseThrow().rawValue());
    }

    @Test
    void recoversMixedEqualsAndColonSeparators() {
        List<String> lines = List.of(
                "app:",
                "  name = polyconf",
                "  version = 1.0.0"
        );

        ParseResult result = new PolyconfParser().parse(lines);
        ConfigSection root = result.root();

        ConfigSection app = root.childSection("app").orElseThrow();
        assertEquals("polyconf", app.childValue("name").orElseThrow().rawValue());
        assertEquals("1.0.0", app.childValue("version").orElseThrow().rawValue());
    }

    @Test
    void recoversDottedKeysWithIndentation() {
        List<String> lines = List.of(
                "database:",
                "  host.name = localhost",
                "  host.port = 5432"
        );

        ParseResult result = new PolyconfParser().parse(lines);
        ConfigSection root = result.root();

        ConfigSection db = root.childSection("database").orElseThrow();
        ConfigSection host = db.childSection("host").orElseThrow();
        assertEquals("localhost", host.childValue("name").orElseThrow().rawValue());
        assertEquals("5432", host.childValue("port").orElseThrow().rawValue());
    }

    @Test
    void recoversDeeplyNestedStructure() {
        List<String> lines = List.of(
                "servers:",
                "  primary:",
                "    host: 192.168.1.1",
                "    port: 80",
                "  backup:",
                "    host: 192.168.1.2",
                "    port: 8080"
        );

        ParseResult result = new PolyconfParser().parse(lines);
        Map<String, Object> flat = result.flattened();

        assertEquals("192.168.1.1", flat.get("servers.primary.host"));
        assertEquals("80", flat.get("servers.primary.port"));
        assertEquals("192.168.1.2", flat.get("servers.backup.host"));
        assertEquals("8080", flat.get("servers.backup.port"));
    }

    @Test
    void fallbackStillYieldsResult() {
        List<String> lines = List.of(
                "key1 = value1",
                "key2 = value2"
        );

        ParseResult result = new PolyconfParser().parse(lines);
        assertNotNull(result);
        assertFalse(result.blocks().isEmpty(), "should produce at least one block");

        Map<String, Object> flat = result.flattened();
        assertEquals("value1", flat.get("key1"));
        assertEquals("value2", flat.get("key2"));
    }

    @Test
    void fallbackHandlesEmptyContent() {
        List<String> lines = List.of();

        ParseResult result = new PolyconfParser().parse(lines);
        assertNotNull(result);
        assertTrue(result.blocks().isEmpty(), "empty content should produce no blocks");
    }
}
