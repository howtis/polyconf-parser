package com.polyconf.parser;

import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KdlResourceTest extends ResourceTestBase {

    @Test
    void app() {
        ParseResult r = parseResource("samples/kdl/app.kdl");
        assertNoErrors(r, "app.kdl");
        assertFormat(r, Format.KDL, "app.kdl");
        Map<String, Object> f = r.flattened();
        assertEquals("myapp", f.get("package.name"));
        assertEquals("1.0.0", f.get("package.version"));
        assertEquals("0.0.0.0", f.get("server.host"));
        assertEquals(8080L, f.get("server.port"));
        assertEquals(4L, f.get("server.workers"));
        assertEquals(30L, f.get("server.timeout.duration"));
        assertEquals("localhost", f.get("database.host"));
        assertEquals(5432L, f.get("database.port"));
        assertEquals("myapp", f.get("database.name"));
        assertEquals("admin", f.get("database.credentials.username"));
        assertEquals("secret", f.get("database.credentials.password"));
        assertEquals(5L, f.get("database.pool.min"));
        assertEquals(20L, f.get("database.pool.max"));
        assertEquals("info", f.get("features.logging.level"));
    }

    @Test
    void complex() {
        ParseResult r = parseResource("samples/kdl/complex.kdl");
        assertNoErrors(r, "complex.kdl");
        assertFormat(r, Format.KDL, "complex.kdl");
        Map<String, Object> f = r.flattened();
        assertEquals("enterprise-app", f.get("package.name"));
        assertEquals("3.2.1", f.get("package.version"));
        assertEquals("Alice", f.get("package.authors.0"));
        assertEquals("Bob", f.get("package.authors.1"));
        assertEquals("alice@corp.com", f.get("package.authors.alice.email"));
        assertEquals("lead", f.get("package.authors.alice.role"));
        assertEquals("bob@corp.com", f.get("package.authors.bob.email"));
        assertEquals("dev", f.get("package.authors.bob.role"));
        assertEquals(1000L, f.get("settings.rate_limit.requests"));
        assertEquals(60L, f.get("settings.rate_limit.period_sec"));
        assertEquals("staging", f.get("environment.name"));
        assertEquals("debug", f.get("environment.log_level"));
    }

    @Test
    void config() {
        ParseResult r = parseResource("samples/kdl/config.kdl");
        assertNoErrors(r, "config.kdl");
        assertFormat(r, Format.KDL, "config.kdl");
        Map<String, Object> f = r.flattened();
        assertEquals("KDL Example", f.get("title"));
        assertEquals("value", f.get("node"));
        assertEquals(8080L, f.get("services.http.port"));
        assertEquals("/health", f.get("services.http.routes.route.path"));
        assertEquals("healthCheck", f.get("services.http.routes.route.handler"));
        assertEquals(9090L, f.get("services.grpc.port"));
        assertEquals(1L, f.get("numbers.0"));
        assertEquals(2L, f.get("numbers.1"));
        assertEquals(3L, f.get("numbers.2"));
        assertEquals(4L, f.get("numbers.3"));
        assertEquals(5L, f.get("numbers.4"));
        assertEquals("Alice", f.get("names.0"));
        assertEquals("Bob", f.get("names.1"));
        assertEquals("Charlie", f.get("names.2"));
        assertEquals(42L, f.get("count"));
        assertEquals(3.14159, (Double) f.get("ratio"), 0.00001);
        assertEquals("hello", f.get("label"));
    }
}
