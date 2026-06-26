package com.polyconf.parser.model;

import org.junit.jupiter.api.Test;
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
}
