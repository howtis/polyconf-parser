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
        assertEquals("8080", f.get("app.server.port"));
        assertEquals("localhost", f.get("database.host"));
        assertEquals("5432", f.get("database.port"));
        assertEquals("myapp", f.get("database.name"));
        assertEquals("10", f.get("database.pool-size"));
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
        assertEquals("10", f.get("akka.actor.deployment./worker.nr-of-instances"));
        assertEquals("127.0.0.1", f.get("akka.remote.artery.canonical.hostname"));
        assertEquals("25520", f.get("akka.remote.artery.canonical.port"));
    }

    @Test
    void config() {
        ParseResult r = parseResource("samples/hocon/config.hocon");
        assertNoErrors(r, "config.hocon");
        assertFormat(r, Format.HOCON, "config.hocon");
        Map<String, Object> f = r.flattened();
        assertEquals("0.0.0.0", f.get("server.host"));
        assertEquals("8080", f.get("server.port"));
        assertEquals("false", f.get("server.debug"));
        assertEquals("localhost", f.get("database.host"));
        assertEquals("5432", f.get("database.port"));
        assertEquals("myapp", f.get("database.name"));
        assertEquals("10", f.get("database.pool-size"));
        assertEquals("admin", f.get("database.credentials.username"));
        assertEquals("secret", f.get("database.credentials.password"));
        assertEquals("3.14", f.get("features.pi"));
        assertEquals("30000", f.get("features.timeout-ms"));
        assertEquals("1000", f.get("features.rate-limit.max-requests"));
        assertEquals("60", f.get("features.rate-limit.window-seconds"));
    }
}
