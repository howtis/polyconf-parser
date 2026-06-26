package com.polyconf.parser;

import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Json5ResourceTest extends ResourceTestBase {

    @Test
    void config() {
        ParseResult r = parseResource("samples/json5/config.json5");
        assertNoErrors(r, "config.json5");
        assertFormat(r, Format.JSON5, "config.json5");
        Map<String, Object> f = r.flattened();
        assertEquals("0.0.0.0", f.get("server.host"));
        assertEquals(8080L, f.get("server.port"));
        assertEquals(false, f.get("server.debug"));
        assertEquals("localhost", f.get("database.host"));
        assertEquals(5432L, f.get("database.port"));
        assertEquals("myapp", f.get("database.name"));
        assertEquals(10L, f.get("database.pool_size"));
        assertEquals("admin", f.get("database.credentials.username"));
        assertEquals("secret", f.get("database.credentials.password"));
        assertEquals(30000L, f.get("features.timeout_ms"));
        assertEquals(3.14, (Double) f.get("features.pi"), 0.001);
    }

    @Test
    void data() {
        ParseResult r = parseResource("samples/json5/data.json5");
        assertNoErrors(r, "data.json5");
        assertFormat(r, Format.JSON5, "data.json5");
        Map<String, Object> f = r.flattened();
        assertEquals(1L, f.get("records[0].id"));
        assertEquals("Alice", f.get("records[0].name"));
        assertEquals(true, f.get("records[0].active"));
        assertEquals(2L, f.get("records[1].id"));
        assertEquals("Bob", f.get("records[1].name"));
        assertEquals(false, f.get("records[1].active"));
        assertEquals(3L, f.get("records[2].id"));
        assertEquals("Charlie", f.get("records[2].name"));
        assertEquals("2024-01-15T10:30:00Z", f.get("metadata.exported"));
        assertEquals(3L, f.get("metadata.count"));
    }

    @Test
    void settings() {
        ParseResult r = parseResource("samples/json5/settings.json5");
        assertNoErrors(r, "settings.json5");
        assertFormat(r, Format.JSON5, "settings.json5");
        Map<String, Object> f = r.flattened();
        assertEquals(42L, f.get("integer"));
        assertEquals(-17L, f.get("negative"));
        assertEquals(255L, f.get("hex"));
        assertEquals(3.14159, (Double) f.get("float"), 0.00001);
        assertEquals(Double.POSITIVE_INFINITY, f.get("infinity"));
        assertEquals(Double.NEGATIVE_INFINITY, f.get("neg_infinity"));
        assertEquals("hello world", f.get("single_quoted"));
        assertEquals("hello\nworld", f.get("double_quoted"));
        assertEquals("valid in JSON5", f.get("unquoted_key"));
        assertEquals(true, f.get("enabled"));
        assertEquals(false, f.get("disabled"));
        assertNull(f.get("empty"));
        assertEquals(1L, f.get("nested.a"));
        assertEquals(2L, f.get("nested.b"));
        assertEquals(3L, f.get("nested.c"));
        assertEquals("first", f.get("items[0]"));
        assertEquals("second", f.get("items[1]"));
        assertEquals("third", f.get("items[2]"));
    }
}
