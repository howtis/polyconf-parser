package com.polyconf.parser;

import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HoconResourceTest extends ResourceTestBase {

    @Test
    void app() {
        ParseResult r = parseResource("samples/hocon/app.hocon");
        assertNoErrors(r, "app.hocon");
        assertFormat(r, Format.HOCON, "app.hocon");
        Map<String, Object> f = r.flattened();
        assertEquals("myapp", f.get("app.name"));
        assertEquals("1.0.0", f.get("app.version"));
        assertEquals("A polyglot configuration parser", f.get("app.description"));
        assertEquals("0.0.0.0", f.get("app.server.host"));
        assertEquals(8080L, f.get("app.server.port"));
        assertEquals("localhost", f.get("database.host"));
        assertEquals(5432L, f.get("database.port"));
        assertEquals("myapp", f.get("database.name"));
        assertEquals(10L, f.get("database.pool-size"));
    }

    @Test
    void akka() {
        ParseResult r = parseResource("samples/hocon/akka.hocon");
        assertNoErrors(r, "akka.hocon");
        assertFormat(r, Format.HOCON, "akka.hocon");
        Map<String, Object> f = r.flattened();
        assertEquals("INFO", f.get("akka.loglevel"));
        assertEquals("WARN", f.get("akka.stdout-loglevel"));
        assertEquals("cluster", f.get("akka.actor.provider"));
        assertEquals(10L, f.get("akka.actor.deployment./worker.nr-of-instances"));
        assertEquals("127.0.0.1", f.get("akka.remote.artery.canonical.hostname"));
        assertEquals(25520L, f.get("akka.remote.artery.canonical.port"));
    }

    @Test
    void config() {
        ParseResult r = parseResource("samples/hocon/config.hocon");
        assertNoErrors(r, "config.hocon");
        assertFormat(r, Format.HOCON, "config.hocon");
        Map<String, Object> f = r.flattened();
        assertEquals("0.0.0.0", f.get("server.host"));
        assertEquals(8080L, f.get("server.port"));
        assertEquals(false, f.get("server.debug"));
        assertEquals("localhost", f.get("database.host"));
        assertEquals(5432L, f.get("database.port"));
        assertEquals("myapp", f.get("database.name"));
        assertEquals(10L, f.get("database.pool-size"));
        assertEquals("admin", f.get("database.credentials.username"));
        assertEquals("secret", f.get("database.credentials.password"));
        assertEquals(3.14, (Double) f.get("features.pi"), 0.001);
        assertEquals(30000L, f.get("features.timeout-ms"));
        assertEquals(1000L, f.get("features.rate-limit.max-requests"));
        assertEquals(60L, f.get("features.rate-limit.window-seconds"));
    }
}
