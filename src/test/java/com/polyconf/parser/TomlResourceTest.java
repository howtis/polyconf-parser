package com.polyconf.parser;

import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TomlResourceTest extends ResourceTestBase {

    @Test
    void simple() {
        ParseResult r = parseResource("samples/toml/simple.toml");
        assertNoErrors(r, "simple.toml");
        assertFormat(r, Format.TOML, "simple.toml");
        Map<String, Object> f = r.flattened();
        assertEquals("TOML Example", f.get("title"));
        assertEquals("1", f.get("version"));
        assertEquals("John Doe", f.get("owner.name"));
        assertEquals("Software developer", f.get("owner.bio"));
        assertEquals("1.1.0", f.get("dependencies.tomlj"));
        assertEquals("2.2", f.get("dependencies.snakeyaml"));
    }

    @Test
    void server() {
        ParseResult r = parseResource("samples/toml/server.toml");
        assertNoErrors(r, "server.toml");
        assertFormat(r, Format.TOML, "server.toml");
        Map<String, Object> f = r.flattened();
        assertEquals("0.0.0.0", f.get("server.host"));
        assertEquals("8080", f.get("server.port"));
        assertEquals("false", f.get("server.debug"));
        assertEquals("localhost", f.get("database.host"));
        assertEquals("5432", f.get("database.port"));
        assertEquals("myapp", f.get("database.name"));
        assertEquals("admin", f.get("database.credentials.username"));
        assertEquals("a.example.com", f.get("servers[0].host"));
        assertEquals("b.example.com", f.get("servers[1].host"));
        assertEquals("1000", f.get("features.rate_limit.max_requests"));
        assertEquals("60", f.get("features.rate_limit.window_seconds"));
    }

    @Test
    void complex() {
        ParseResult r = parseResource("samples/toml/complex.toml");
        assertNoErrors(r, "complex.toml");
        assertFormat(r, Format.TOML, "complex.toml");
        Map<String, Object> f = r.flattened();
        assertEquals("Complex TOML", f.get("title"));
        assertEquals("Tom Preston-Werner", f.get("owner.name"));
        assertEquals("true", f.get("database.enabled"));
        assertEquals("8000", f.get("database.ports[0]"));
        assertEquals("8001", f.get("database.ports[1]"));
        assertEquals("8002", f.get("database.ports[2]"));
        assertEquals("10.0.0.1", f.get("servers.alpha.ip"));
        assertEquals("frontend", f.get("servers.alpha.role"));
        assertEquals("10.0.0.2", f.get("servers.beta.ip"));
        assertEquals("backend", f.get("servers.beta.role"));
        assertEquals("Hammer", f.get("products[0].name"));
        assertEquals("738594937", f.get("products[0].sku"));
        assertEquals("Nail", f.get("products[1].name"));
        assertEquals("284758393", f.get("products[1].sku"));
    }

    @Test
    void cargo() {
        ParseResult r = parseResource("samples/toml/cargo.toml");
        assertNoErrors(r, "cargo.toml");
        assertFormat(r, Format.TOML, "cargo.toml");
        Map<String, Object> f = r.flattened();
        assertEquals("polyconf-parser", f.get("package.name"));
        assertEquals("0.1.0", f.get("package.version"));
        assertEquals("2021", f.get("package.edition"));
        assertEquals("MIT", f.get("package.license"));
        assertEquals("1.0", f.get("dependencies.serde.version"));
        assertEquals("1.0", f.get("dependencies.serde_json"));
        assertEquals("0.8", f.get("dependencies.toml"));
        assertEquals("0.5", f.get("dev-dependencies.criterion.version"));
        assertEquals("3", f.get("profile.release.opt-level"));
        assertEquals("true", f.get("profile.release.lto"));
    }

    @Test
    void pyproject() {
        ParseResult r = parseResource("samples/toml/pyproject.toml");
        assertNoErrors(r, "pyproject.toml");
        assertFormat(r, Format.TOML, "pyproject.toml");
        Map<String, Object> f = r.flattened();
        assertEquals("hatchling.build", f.get("build-system.build-backend"));
        assertEquals("polyconf-parser", f.get("project.name"));
        assertEquals("0.1.0", f.get("project.version"));
        assertEquals(">=3.9", f.get("project.requires-python"));
        assertEquals("100", f.get("tool.ruff.line-length"));
        assertEquals("py39", f.get("tool.ruff.target-version"));
        assertEquals("true", f.get("tool.mypy.strict"));
        assertEquals("8.0", f.get("tool.pytest.ini_options.minversion"));
    }

    @Test
    void dotted() {
        ParseResult r = parseResource("samples/toml/dotted.toml");
        assertNoErrors(r, "dotted.toml");
        assertFormat(r, Format.TOML, "dotted.toml");
        Map<String, Object> f = r.flattened();
        assertEquals("Orange", f.get("name"));
        assertEquals("orange", f.get("physical.color"));
        assertEquals("round", f.get("physical.shape"));
        assertEquals("0.0.0.0", f.get("server.host"));
        assertEquals("8080", f.get("server.port"));
        assertEquals("false", f.get("server.debug"));
        assertEquals("localhost", f.get("database.host"));
        assertEquals("5432", f.get("database.port"));
        assertEquals("admin", f.get("database.credentials.username"));
        assertEquals("secret", f.get("database.credentials.password"));
    }

    @Test
    void inline() {
        ParseResult r = parseResource("samples/toml/inline.toml");
        assertHasBlock(r, "inline.toml");
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "inline.toml should produce non-empty output");
        assertEquals("TOML Inline Table Example", f.get("title"));
        assertTrue(f.get("point.x") != null || f.containsKey("point"),
                "point should be present");
        assertTrue(f.get("server.host") != null || f.containsKey("server"),
                "server should be present");
    }
}
