package com.polyconf.parser;

import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MixedResourceTest extends ResourceTestBase {

    @Test
    void hintedAllFormats() {
        ParseResult r = parseResource("mixed/hinted-all-formats.txt");
        assertHasBlock(r, "hinted-all-formats.txt");
        Map<String, Object> f = r.flattened();
        assertNotNull(f);
        assertEquals("Widget", f.get("products[0].name"));
        assertEquals(9.99, (Double) f.get("products[0].price"), 0.001);
        assertEquals("DEBUG", f.get("akka.loglevel"));
        assertEquals("DEBUG", f.get("log.level"));
        assertEquals("/var/log/app.log", f.get("log.file"));
        assertEquals("redis", f.get("cache.backend"));
        assertEquals("300", f.get("cache.ttl"));
        assertEquals("development", f.get("NODE_ENV"));
        assertEquals("/var/data", f.get("paths.data"));
        assertEquals("/var/log", f.get("paths.logs"));
    }

    @Test
    void hintedMany() {
        ParseResult r = parseResource("mixed/hinted-many.txt");
        assertHasBlock(r, "hinted-many.txt");
        Map<String, Object> f = r.flattened();
        assertNotNull(f);
        assertEquals("localhost", f.get("database.host"));
        assertEquals("5432", f.get("database.port"));
        assertEquals("0.0.0.0", f.get("server.host"));
        assertEquals("8080", f.get("server.port"));
        assertEquals("debug", f.get("logging.level"));
        assertEquals("3600", f.get("cache.ttl"));
        assertEquals("1024", f.get("cache.maxSize"));
        assertEquals("sk-1234567890", f.get("API_KEY"));
        assertEquals("true", f.get("metrics.enabled"));
        assertEquals("smtp.example.com", f.get("email.smtp_host"));
        assertEquals("587", f.get("email.smtp_port"));
    }

    @Test
    void hintedMixed() {
        ParseResult r = parseResource("mixed/hinted-mixed.txt");
        assertHasBlock(r, "hinted-mixed.txt");
        Map<String, Object> f = r.flattened();
        assertEquals("0.0.0.0", f.get("server.host"));
        assertEquals("8080", f.get("server.port"));
    }

    @Test
    void realWorldMicroservice() {
        ParseResult r = parseResource("mixed/real-world-microservice.txt");
        assertHasBlock(r, "real-world-microservice.txt");
        Map<String, Object> f = r.flattened();
        assertNotNull(f);
        assertEquals("order-service", f.get("app.name"));
        assertEquals("2.1.0", f.get("app.version"));
        assertEquals("3.3.0", f.get("spring.boot.version"));
        assertEquals("20", f.get("spring.datasource.hikari.maximum-pool-size"));
        assertEquals("postgres-primary.internal", f.get("DB_HOST"));
        assertEquals("5432", f.get("DB_PORT"));
        assertEquals("production", f.get("SPRING_PROFILES_ACTIVE"));
        assertTrue(f.containsKey("metrics.enabled") || f.containsKey("enabled"),
                "unhinted metrics block should produce output");
    }

    @Test
    void unhintedMany() {
        ParseResult r = parseResource("mixed/unhinted-many.txt");
        assertHasBlock(r, "unhinted-many.txt");
        Map<String, Object> f = r.flattened();
        assertNotNull(f);
        assertFalse(f.isEmpty(), "unhinted-many.txt should produce non-empty output");
        assertTrue(f.containsKey("server.port") || f.containsKey("config.app.name")
                || f.containsKey("database.host") || f.containsKey("app.debug")
                || f.containsKey("redis.host") || f.containsKey("REDIS_URL")
                || f.containsKey("metrics.enabled"),
                "At least one identifiable key should be present");
    }

    @Test
    void unhintedMixed() {
        ParseResult r = parseResource("mixed/unhinted-mixed.txt");
        assertHasBlock(r, "unhinted-mixed.txt");
        Map<String, Object> f = r.flattened();
        assertNotNull(f);
        assertFalse(f.isEmpty(), "unhinted-mixed.txt should produce non-empty output");
        assertTrue(f.containsKey("name") || f.containsKey("server.host")
                || f.containsKey("config.debug"),
                "At least one key should be present from unhinted-mixed blocks");
    }

    @Test
    void ambiguousTomlIni() {
        ParseResult r = parseResource("mixed/ambiguous-toml-ini.txt");
        assertHasBlock(r, "ambiguous-toml-ini.txt");
        Map<String, Object> f = r.flattened();
        assertEquals("localhost", f.get("server.host"));
    }

    @Test
    void ambiguousTomlProperties() {
        ParseResult r = parseResource("mixed/ambiguous-toml-properties.txt");
        assertHasBlock(r, "ambiguous-toml-properties.txt");
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "ambiguous-toml-properties.txt should have flattened output");
        assertEquals("localhost,                 staging.example.com,                 production.example.com",
                f.get("allowed.hosts"));
        assertTrue(f.containsKey("welcome.message"), "welcome.message should be present");
    }

    @Test
    void consecutiveSameFormat() {
        ParseResult r = parseResource("mixed/consecutive-same-format.txt");
        assertNoErrors(r, "consecutive-same-format.txt");
        assertHasBlock(r, "consecutive-same-format.txt");
        assertNotNull(r.flattened());
    }
}
