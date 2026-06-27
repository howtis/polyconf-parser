package com.polyconf.parser.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfigNodeTest {

    @Test
    void configValueAccessors() {
        Provenance p = new Provenance(1, Format.TOML, "host = \"localhost\"", 1.0);
        ConfigValue value = new ConfigValue("host", "localhost", ValueType.STRING, p, "server.host");

        assertEquals("host", value.key());
        assertEquals("localhost", value.rawValue());
        assertEquals(ValueType.STRING, value.type());
        assertTrue(value.asString().isPresent());
        assertEquals("localhost", value.asString().get());
        assertEquals("localhost", value.get());
        assertFalse(value.isNull());
    }

    @Test
    void configValueIntAndFloatCoercion() {
        Provenance p = new Provenance(2, Format.TOML, "port = 5432", 1.0);
        ConfigValue intValue = new ConfigValue("port", 5432, ValueType.INTEGER, p, "server.port");

        assertTrue(intValue.asInt().isPresent());
        assertEquals(5432, intValue.asInt().get());
        assertTrue(intValue.asFloat().isPresent());
        assertEquals(5432.0, intValue.asFloat().get());
    }

    @Test
    void configValueBoolCoercion() {
        Provenance p = new Provenance(3, Format.TOML, "enabled = true", 1.0);
        ConfigValue boolValue = new ConfigValue("enabled", true, ValueType.BOOLEAN, p, "server.enabled");

        assertTrue(boolValue.asBool().isPresent());
        assertTrue(boolValue.asBool().get());
    }

    @Test
    void configValueNullHandling() {
        Provenance p = new Provenance(4, Format.UNKNOWN, "key = null", 0.5);
        ConfigValue nullValue = new ConfigValue("key", null, ValueType.NULL, p, "key");

        assertTrue(nullValue.isNull());
        assertFalse(nullValue.asString().isPresent());
        assertFalse(nullValue.asInt().isPresent());
    }

    @Test
    void configSectionTreeResolve() {
        Provenance p = new Provenance(0, Format.TOML, "[server]", 1.0);
        ConfigSection root = new ConfigSection("", p, "");

        Provenance pHost = new Provenance(1, Format.TOML, "host = \"localhost\"", 1.0);
        ConfigValue host = new ConfigValue("host", "localhost", ValueType.STRING, pHost, "server.host");

        ConfigSection server = new ConfigSection("server", p, "server");
        server.children().put("host", host);
        root.children().put("server", server);

        Optional<ConfigNode> resolved = root.resolve("server.host");
        assertTrue(resolved.isPresent());
        assertInstanceOf(ConfigValue.class, resolved.get());
        assertEquals("localhost", ((ConfigValue) resolved.get()).rawValue());
    }

    // --- ConfigSection self-value ---

    @Test
    void sectionHasSelfValueWithTextMarker() {
        Provenance p = new Provenance(0, Format.XML, "", 1.0);
        ConfigSection section = new ConfigSection("name", p, "name");
        section.children().put("#text", new ConfigValue("#text", "hello", ValueType.STRING, p, "name.#text"));
        assertTrue(section.hasSelfValue());
    }

    @Test
    void sectionHasSelfValueWithSelfMarker() {
        Provenance p = new Provenance(0, Format.PROPERTIES, "", 1.0);
        ConfigSection section = new ConfigSection("db", p, "db");
        section.children().put("#self", new ConfigValue("#self", "fallback", ValueType.STRING, p, "db.#self"));
        assertTrue(section.hasSelfValue());
    }

    @Test
    void sectionHasSelfValueFalseWhenNoMarker() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection section = new ConfigSection("server", p, "server");
        section.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p, "server.host"));
        assertFalse(section.hasSelfValue());
    }

    @Test
    void sectionSelfValueGetsTextMarker() {
        Provenance p = new Provenance(0, Format.XML, "", 1.0);
        ConfigSection section = new ConfigSection("name", p, "name");
        ConfigValue textValue = new ConfigValue("#text", "hello", ValueType.STRING, p, "name.#text");
        section.children().put("#text", textValue);
        assertTrue(section.selfValue().isPresent());
        assertEquals("hello", ((ConfigValue) section.selfValue().get()).asString().orElseThrow());
    }

    @Test
    void sectionSelfValueGetsSelfMarker() {
        Provenance p = new Provenance(0, Format.PROPERTIES, "", 1.0);
        ConfigSection section = new ConfigSection("db", p, "db");
        ConfigValue selfValue = new ConfigValue("#self", "fallback", ValueType.STRING, p, "db.#self");
        section.children().put("#self", selfValue);
        assertTrue(section.selfValue().isPresent());
        assertEquals("fallback", ((ConfigValue) section.selfValue().get()).asString().orElseThrow());
    }

    @Test
    void sectionSelfValueEmptyWhenNoMarker() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection section = new ConfigSection("server", p, "server");
        section.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p, "server.host"));
        assertFalse(section.selfValue().isPresent());
    }

    @Test
    void flattenTreatsSelfValueAsLeaf() {
        Provenance p = new Provenance(0, Format.XML, "", 1.0);
        ConfigSection root = new ConfigSection("", p, "");
        ConfigSection name = new ConfigSection("name", p, "name");
        name.children().put("#text", new ConfigValue("#text", "John", ValueType.STRING, p, "name.#text"));
        root.children().put("name", name);

        ConfigAccessor accessor = new ConfigAccessor(root);
        Map<String, Object> flat = accessor.asFlattenedMap();

        assertEquals("John", flat.get("name"));
        assertFalse(flat.containsKey("name.#text"));
    }

    @Test
    void configAccessorFlattenedMap() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection root = new ConfigSection("", p, "");

        ConfigValue host = new ConfigValue("host", "localhost", ValueType.STRING,
                new Provenance(1, Format.TOML, "host = \"localhost\"", 1.0), "host");
        ConfigValue port = new ConfigValue("port", 5432, ValueType.INTEGER,
                new Provenance(2, Format.TOML, "port = 5432", 1.0), "port");

        root.children().put("host", host);
        root.children().put("port", port);

        ConfigAccessor accessor = new ConfigAccessor(root);
        Map<String, Object> flat = accessor.asFlattenedMap();

        assertEquals("localhost", flat.get("host"));
        assertEquals(5432, flat.get("port"));
    }

    @Test
    void configAccessorTypedAccess() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection root = new ConfigSection("", p, "");

        ConfigSection server = new ConfigSection("server", p, "server");
        server.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p, "server.host"));
        server.children().put("port", new ConfigValue("port", 5432, ValueType.INTEGER, p, "server.port"));
        server.children().put("enabled", new ConfigValue("enabled", true, ValueType.BOOLEAN, p, "server.enabled"));
        root.children().put("server", server);

        ConfigAccessor accessor = new ConfigAccessor(root);

        assertTrue(accessor.getString("server.host").isPresent());
        assertEquals("localhost", accessor.getString("server.host").get());
        assertEquals(5432, accessor.getInt("server.port").get());
        assertTrue(accessor.getBool("server.enabled").get());
    }

    @Test
    void configAccessorContainsKey() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection root = new ConfigSection("", p, "");

        ConfigSection server = new ConfigSection("server", p, "server");
        server.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p, "server.host"));
        server.children().put("port", new ConfigValue("port", 5432, ValueType.INTEGER, p, "server.port"));
        root.children().put("server", server);
        root.children().put("name", new ConfigValue("name", "test", ValueType.STRING, p, "name"));

        ConfigAccessor accessor = new ConfigAccessor(root);

        assertTrue(accessor.containsKey("name"));
        assertTrue(accessor.containsKey("server"));
        assertTrue(accessor.containsKey("server.host"));
        assertTrue(accessor.containsKey("server.port"));
        assertFalse(accessor.containsKey("missing"));
        assertFalse(accessor.containsKey("server.missing"));
        assertFalse(accessor.containsKey("server.host.extra"));
    }

    @Test
    void configAccessorContainsKeyKdlPureContainer() {
        Provenance p = new Provenance(0, Format.KDL, "", 1.0);
        ConfigSection root = new ConfigSection("", p, "");

        ConfigSection server = new ConfigSection("server", p, "server");
        server.children().put("host", new ConfigValue("host", "0.0.0.0", ValueType.STRING, p, "server.host"));
        server.children().put("port", new ConfigValue("port", 8080, ValueType.INTEGER, p, "server.port"));
        root.children().put("server", server);

        ConfigAccessor accessor = new ConfigAccessor(root);

        assertTrue(accessor.containsKey("server"));
        assertTrue(accessor.containsKey("server.host"));
        assertFalse(accessor.containsKey("missing"));
    }

    @Test
    void parseResultSuccessPath() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection root = new ConfigSection("", p, "");
        root.children().put("key", new ConfigValue("key", "value", ValueType.STRING, p, "key"));

        BlockResult block = new BlockResult(0, 1, Format.TOML, 1.0, true, root);
        ParseResult result = new ParseResult(root, java.util.List.of(block), java.util.List.of());

        assertFalse(result.hasWarnings());
        assertFalse(result.hasErrors());

        Map<String, Object> flat = result.flattened();
        assertEquals("value", flat.get("key"));
    }

    @Test
    void parseResultWithDiagnostics() {
        Provenance p = new Provenance(0, Format.UNKNOWN, "", 0.0);
        ConfigSection root = new ConfigSection("", p, "");

        BlockDiagnostic warning = new BlockDiagnostic(0, 1, "Ambiguous format", DiagnosticLevel.WARNING);
        BlockDiagnostic error = new BlockDiagnostic(2, 3, "Parse failed", DiagnosticLevel.ERROR);

        ParseResult result = new ParseResult(root,
                java.util.List.of(),
                java.util.List.of(warning, error));

        assertTrue(result.hasWarnings());
        assertTrue(result.hasErrors());
    }

    @Test
    void provenanceValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new Provenance(-1, Format.UNKNOWN, "", 0.0));
        assertThrows(IllegalArgumentException.class, () ->
                new Provenance(0, Format.UNKNOWN, "", 1.5));
        assertThrows(IllegalArgumentException.class, () ->
                new Provenance(0, Format.UNKNOWN, "", -0.1));
    }

    // --- ConfigValue coercion edge cases ---

    @Test
    void asIntFromFloatTruncates() {
        Provenance p = new Provenance(1, Format.JSON, "pi", 1.0);
        ConfigValue val = new ConfigValue("pi", 3.14, ValueType.FLOAT, p, "pi");
        assertEquals(3, val.asInt().orElseThrow());
    }

    @Test
    void asIntFromStringNumeric() {
        Provenance p = new Provenance(1, Format.JSON, "port", 1.0);
        ConfigValue val = new ConfigValue("port", "5432", ValueType.STRING, p, "port");
        assertEquals(5432, val.asInt().orElseThrow());
    }

    @Test
    void asIntFromStringNonNumeric() {
        Provenance p = new Provenance(1, Format.JSON, "name", 1.0);
        ConfigValue val = new ConfigValue("name", "hello", ValueType.STRING, p, "name");
        assertFalse(val.asInt().isPresent());
    }

    @Test
    void asBoolFromStringTrue() {
        Provenance p = new Provenance(1, Format.JSON, "active", 1.0);
        ConfigValue val = new ConfigValue("active", "true", ValueType.STRING, p, "active");
        assertTrue(val.asBool().orElseThrow());
    }

    @Test
    void asBoolFromStringFalse() {
        Provenance p = new Provenance(1, Format.JSON, "debug", 1.0);
        ConfigValue val = new ConfigValue("debug", "false", ValueType.STRING, p, "debug");
        assertFalse(val.asBool().orElseThrow());
    }

    @Test
    void asBoolFromStringNonBoolean() {
        Provenance p = new Provenance(1, Format.JSON, "mode", 1.0);
        ConfigValue val = new ConfigValue("mode", "maybe", ValueType.STRING, p, "mode");
        assertFalse(val.asBool().isPresent());
    }

    @Test
    void asFloatFromInteger() {
        Provenance p = new Provenance(1, Format.JSON, "count", 1.0);
        ConfigValue val = new ConfigValue("count", 42, ValueType.INTEGER, p, "count");
        assertEquals(42.0, val.asFloat().orElseThrow(), 0.001);
    }

    @Test
    void asFloatFromStringNumeric() {
        Provenance p = new Provenance(1, Format.JSON, "pi", 1.0);
        ConfigValue val = new ConfigValue("pi", "3.14", ValueType.STRING, p, "pi");
        assertEquals(3.14, val.asFloat().orElseThrow(), 0.001);
    }

    @Test
    void asFloatFromStringNonNumeric() {
        Provenance p = new Provenance(1, Format.JSON, "label", 1.0);
        ConfigValue val = new ConfigValue("label", "hello", ValueType.STRING, p, "label");
        assertFalse(val.asFloat().isPresent());
    }

    // --- ConfigAccessor additional methods ---

    @Test
    void configAccessorGetFloat() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection root = new ConfigSection("", p, "");
        root.children().put("pi", new ConfigValue("pi", 3.14, ValueType.FLOAT, p, "pi"));

        ConfigAccessor accessor = new ConfigAccessor(root);
        assertEquals(3.14, accessor.getFloat("pi").orElseThrow(), 0.001);
        assertFalse(accessor.getFloat("missing").isPresent());
    }

    @Test
    void configAccessorGetSection() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection root = new ConfigSection("", p, "");
        ConfigSection server = new ConfigSection("server", p, "server");
        ConfigSection db = new ConfigSection("db", p, "server.db");
        server.children().put("db", db);
        root.children().put("server", server);

        ConfigAccessor accessor = new ConfigAccessor(root);
        assertTrue(accessor.getSection("server").isPresent());
        assertTrue(accessor.getSection("server.db").isPresent());
        assertFalse(accessor.getSection("missing").isPresent());
    }

    @Test
    void configAccessorGet() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection root = new ConfigSection("", p, "");
        root.children().put("key", new ConfigValue("key", 42, ValueType.INTEGER, p, "key"));

        ConfigAccessor accessor = new ConfigAccessor(root);
        assertTrue(accessor.get("key").isPresent());
        assertEquals(42, ((ConfigValue) accessor.get("key").get()).rawValue());
        assertFalse(accessor.get("missing").isPresent());
    }

    @Test
    void configAccessorWalk() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection root = new ConfigSection("", p, "");
        root.children().put("a", new ConfigValue("a", 1, ValueType.INTEGER, p, "a"));
        root.children().put("b", new ConfigValue("b", 2, ValueType.INTEGER, p, "b"));

        ConfigAccessor accessor = new ConfigAccessor(root);
        List<ConfigNode> nodes = accessor.walk().toList();
        // root + 2 children = 3 nodes
        assertEquals(3, nodes.size());
    }

    @Test
    void configAccessorNullRootThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConfigAccessor(null));
    }

    // --- BlockResult validation ---

    @Test
    void blockResultNegativeStartLineThrows() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection section = new ConfigSection("", p, "");
        assertThrows(IllegalArgumentException.class, () ->
                new BlockResult(-1, 0, Format.TOML, 1.0, false, section));
    }

    @Test
    void blockResultEndLineBeforeStartLineThrows() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection section = new ConfigSection("", p, "");
        assertThrows(IllegalArgumentException.class, () ->
                new BlockResult(5, 3, Format.TOML, 1.0, false, section));
    }

    @Test
    void blockResultConfidenceBelowRangeThrows() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection section = new ConfigSection("", p, "");
        assertThrows(IllegalArgumentException.class, () ->
                new BlockResult(0, 1, Format.TOML, -0.1, false, section));
    }

    @Test
    void blockResultConfidenceAboveRangeThrows() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection section = new ConfigSection("", p, "");
        assertThrows(IllegalArgumentException.class, () ->
                new BlockResult(0, 1, Format.TOML, 1.01, false, section));
    }

    @Test
    void blockResultNullFormatThrows() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection section = new ConfigSection("", p, "");
        assertThrows(IllegalArgumentException.class, () ->
                new BlockResult(0, 1, null, 1.0, false, section));
    }

    @Test
    void blockResultNullSectionThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new BlockResult(0, 1, Format.TOML, 1.0, false, null));
    }

    // --- BlockDiagnostic validation ---

    @Test
    void blockDiagnosticNegativeStartLineThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new BlockDiagnostic(-1, 0, "msg", DiagnosticLevel.ERROR));
    }

    @Test
    void blockDiagnosticEndLineBeforeStartLineThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new BlockDiagnostic(3, 1, "msg", DiagnosticLevel.WARNING));
    }

    @Test
    void blockDiagnosticNullMessageThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new BlockDiagnostic(0, 1, null, DiagnosticLevel.ERROR));
    }

    @Test
    void blockDiagnosticBlankMessageThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new BlockDiagnostic(0, 1, "   ", DiagnosticLevel.ERROR));
    }

    // --- ParserResult validation ---

    @Test
    void parserResultNullSectionThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new ParserResult(null, List.of()));
    }

    @Test
    void parserResultNullDiagnosticsThrows() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection section = new ConfigSection("", p, "");
        assertThrows(IllegalArgumentException.class, () ->
                new ParserResult(section, null));
    }

    @Test
    void parserResultOkFactory() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection section = new ConfigSection("", p, "");
        ParserResult result = ParserResult.ok(section);
        assertSame(section, result.section());
        assertTrue(result.diagnostics().isEmpty());
    }

    // --- ConfigNode.resolve for ConfigValue/ConfigList ---

    @Test
    void configValueResolveEmptyPath() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigValue val = new ConfigValue("key", 42, ValueType.INTEGER, p, "key");
        assertTrue(val.resolve(null).isPresent());
        assertTrue(val.resolve("").isPresent());
        assertSame(val, val.resolve(null).get());
    }

    @Test
    void configValueResolveNonEmptyPath() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigValue val = new ConfigValue("key", 42, ValueType.INTEGER, p, "key");
        assertFalse(val.resolve("any.path").isPresent());
    }

    @Test
    void configListResolve() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigList list = new ConfigList("items", List.of(), p, "items");
        assertTrue(list.resolve(null).isPresent());
        assertFalse(list.resolve("any.path").isPresent());
    }

    // --- ParseResult validation ---

    @Test
    void parseResultNullRootThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new ParseResult(null, List.of(), List.of()));
    }

    @Test
    void parseResultNullBlocksThrows() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection root = new ConfigSection("", p, "");
        assertThrows(IllegalArgumentException.class, () ->
                new ParseResult(root, null, List.of()));
    }

    @Test
    void parseResultNullDiagnosticsThrows() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection root = new ConfigSection("", p, "");
        assertThrows(IllegalArgumentException.class, () ->
                new ParseResult(root, List.of(), null));
    }
}
