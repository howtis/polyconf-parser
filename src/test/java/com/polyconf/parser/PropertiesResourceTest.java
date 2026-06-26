package com.polyconf.parser;

import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesResourceTest extends ResourceTestBase {

    @Test
    void app() {
        ParseResult r = parseResource("samples/properties/app.properties");
        assertNoErrors(r, "app.properties");
        assertFormat(r, Format.PROPERTIES, "app.properties");
        Map<String, Object> f = r.flattened();
        assertEquals("polyconf", f.get("app.name"));
        assertEquals("1.0.0", f.get("app.version"));
        assertEquals("A polyglot configuration parser", f.get("app.description"));
        assertEquals("0.0.0.0", f.get("server.host"));
        assertEquals(8080L, f.get("server.port"));
        assertEquals(false, f.get("server.debug"));
        assertEquals("localhost", f.get("database.host"));
        assertEquals(5432L, f.get("database.port"));
        assertEquals("myapp", f.get("database.name"));
        assertEquals(10L, f.get("database.pool.size"));
        assertEquals(true, f.get("features.auth.enabled"));
        assertEquals(30000L, f.get("features.timeout.ms"));
    }

    @Test
    void database() {
        ParseResult r = parseResource("samples/properties/database.properties");
        assertNoErrors(r, "database.properties");
        assertFormat(r, Format.PROPERTIES, "database.properties");
        Map<String, Object> f = r.flattened();
        assertEquals("jdbc:postgresql://localhost:5432/mydb", f.get("db.url"));
        assertEquals("admin", f.get("db.username"));
        assertEquals(5L, f.get("db.pool.min"));
        assertEquals(20L, f.get("db.pool.max"));
        assertEquals(30000L, f.get("db.pool.timeout"));
        assertEquals(true, f.get("db.ssl"));
        assertEquals("require", f.get("db.ssl.mode"));
    }

    @Test
    void logging() {
        ParseResult r = parseResource("samples/properties/logging.properties");
        assertNoErrors(r, "logging.properties");
        assertFormat(r, Format.PROPERTIES, "logging.properties");
        Map<String, Object> f = r.flattened();
        assertEquals("INFO", f.get("log.level"));
        assertEquals("json", f.get("log.format"));
        assertEquals("file", f.get("log.output"));
        assertEquals("/var/log/app/app.log", f.get("log.file.path"));
        assertEquals("10MB", f.get("log.file.max-size"));
        assertEquals(5L, f.get("log.file.max-backups"));
        assertEquals(true, f.get("log.file.compress"));
        assertEquals(true, f.get("log.console.enabled"));
        assertEquals("DEBUG", f.get("log.logger.com.example.app"));
        assertEquals("WARN", f.get("log.logger.org.springframework"));
    }

    @Test
    void gradle() {
        ParseResult r = parseResource("samples/properties/gradle.properties");
        assertNoErrors(r, "gradle.properties");
        assertFormat(r, Format.PROPERTIES, "gradle.properties");
        Map<String, Object> f = r.flattened();
        assertEquals(true, f.get("org.gradle.parallel"));
        assertEquals(true, f.get("org.gradle.caching"));
        assertEquals(true, f.get("org.gradle.daemon"));
        assertEquals("1.2.0-SNAPSHOT", f.get("version"));
        assertEquals("com.polyconf", f.get("group"));
        assertEquals(17L, f.get("sourceCompatibility"));
        assertEquals(17L, f.get("targetCompatibility"));
    }

    @Test
    void escapes() {
        ParseResult r = parseResource("samples/properties/escapes.properties");
        assertHasBlock(r, "escapes.properties");
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "escapes.properties should produce non-empty output");
        assertEquals("My Application", f.get("app.name"));
        assertTrue(f.get("app") != null || f.containsKey("app.description")
                || f.containsKey("paths.windows") || f.containsKey("paths.unix"),
                "escapes.properties should contain path or app data");
    }

    @Test
    void i18n() {
        ParseResult r = parseResource("samples/properties/i18n.properties");
        assertHasBlock(r, "i18n.properties");
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "i18n.properties should produce non-empty output");
        assertEquals("MyApp", f.get("app.name"));
        assertTrue(f.containsKey("app") || f.containsKey("greeting.japanese")
                || f.containsKey("currency.euro"),
                "i18n.properties should contain i18n keys");
    }
}
