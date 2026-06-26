package com.polyconf.parser;

import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonResourceTest extends ResourceTestBase {

    @Test
    void config() {
        ParseResult r = parseResource("samples/json/config.json");
        assertNoErrors(r, "config.json");
        assertFormat(r, Format.JSON, "config.json");
        Map<String, Object> f = r.flattened();
        assertEquals("0.0.0.0", f.get("server.host"));
        assertEquals(8080L, f.get("server.port"));
        assertEquals(false, f.get("server.debug"));
        assertEquals("localhost", f.get("database.host"));
        assertEquals(5432L, f.get("database.port"));
        assertEquals("myapp", f.get("database.name"));
        assertEquals(10L, f.get("database.pool_size"));
    }

    @Test
    void manifest() {
        ParseResult r = parseResource("samples/json/manifest.json");
        assertNoErrors(r, "manifest.json");
        assertFormat(r, Format.JSON, "manifest.json");
        Map<String, Object> f = r.flattened();
        assertEquals(3L, f.get("manifest_version"));
        assertEquals("My Extension", f.get("name"));
        assertEquals("1.0.0", f.get("version"));
        assertEquals("A browser extension example", f.get("description"));
        assertEquals("background.js", f.get("background.service_worker"));
        assertEquals("popup.html", f.get("action.default_popup"));
    }

    @Test
    void packageJson() {
        ParseResult r = parseResource("samples/json/package.json");
        assertNoErrors(r, "package.json");
        assertFormat(r, Format.JSON, "package.json");
        Map<String, Object> f = r.flattened();
        assertEquals("my-web-app", f.get("name"));
        assertEquals("2.1.0", f.get("version"));
        assertEquals("A sample web application", f.get("description"));
        assertEquals("index.js", f.get("main"));
        assertTrue(((String) f.get("scripts.start")).contains("node"));
        assertEquals("MIT", f.get("license"));
        assertEquals("Jane Smith", f.get("author"));
    }

    @Test
    void tsconfig() {
        ParseResult r = parseResource("samples/json/tsconfig.json");
        assertNoErrors(r, "tsconfig.json");
        assertFormat(r, Format.JSON, "tsconfig.json");
        Map<String, Object> f = r.flattened();
        assertEquals("ES2022", f.get("compilerOptions.target"));
        assertEquals("NodeNext", f.get("compilerOptions.module"));
        assertEquals(true, f.get("compilerOptions.strict"));
        assertEquals(true, f.get("compilerOptions.sourceMap"));
        assertEquals("./dist", f.get("compilerOptions.outDir"));
        assertEquals("./src", f.get("compilerOptions.rootDir"));
        assertEquals("src/**/*.ts", f.get("include[0]"));
        assertEquals("src/**/*.tsx", f.get("include[1]"));
    }
}
