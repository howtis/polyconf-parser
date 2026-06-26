package com.polyconf.parser;

import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IniResourceTest extends ResourceTestBase {

    @Test
    void config() {
        ParseResult r = parseResource("samples/ini/config.ini");
        assertNoErrors(r, "config.ini");
        assertFormat(r, Format.INI, "config.ini");
        Map<String, Object> f = r.flattened();
        assertEquals("0.0.0.0", f.get("server.host"));
        assertEquals("8080", f.get("server.port"));
        assertEquals("false", f.get("server.debug"));
        assertEquals("localhost", f.get("database.host"));
        assertEquals("5432", f.get("database.port"));
        assertEquals("myapp", f.get("database.name"));
        assertEquals("true", f.get("features.auth"));
        assertEquals("true", f.get("features.logging"));
        assertEquals("true", f.get("features.metrics"));
        assertEquals("30000", f.get("features.timeout_ms"));
    }

    @Test
    void database() {
        ParseResult r = parseResource("samples/ini/database.ini");
        assertNoErrors(r, "database.ini");
        assertFormat(r, Format.INI, "database.ini");
        Map<String, Object> f = r.flattened();
        assertEquals("db.example.com", f.get("client.host"));
        assertEquals("3306", f.get("client.port"));
        assertEquals("myapp", f.get("client.database"));
        assertEquals("app_user", f.get("client.user"));
        assertEquals("utf8mb4", f.get("client.charset"));
        assertEquals("/etc/ssl/ca-cert.pem", f.get("client.ssl.ca"));
        assertEquals("0.0.0.0", f.get("server.bind-address"));
        assertEquals("200", f.get("server.max_connections"));
        assertEquals("2G", f.get("server.innodb_buffer_pool_size"));
    }

    @Test
    void game() {
        ParseResult r = parseResource("samples/ini/game.ini");
        assertNoErrors(r, "game.ini");
        assertFormat(r, Format.INI, "game.ini");
        Map<String, Object> f = r.flattened();
        assertEquals("Super Adventure", f.get("Game.title"));
        assertEquals("1.2.3", f.get("Game.version"));
        assertEquals("4", f.get("Game.max_players"));
        assertEquals("normal", f.get("Game.difficulty"));
        assertEquals("1920x1080", f.get("Graphics.resolution"));
        assertEquals("true", f.get("Graphics.fullscreen"));
        assertEquals("true", f.get("Graphics.vsync"));
        assertEquals("0.8", f.get("Audio.master_volume"));
        assertEquals("0.6", f.get("Audio.music_volume"));
        assertEquals("1.0", f.get("Audio.sfx_volume"));
    }

    @Test
    void php() {
        ParseResult r = parseResource("samples/ini/php.ini");
        assertHasBlock(r, "php.ini");
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "php.ini should produce non-empty flattened output");
        assertTrue(f.containsKey("PHP.engine") || f.containsKey("engine"),
                "php.ini should contain engine setting from [PHP] section");
        assertTrue(f.containsKey("Session.session.save_handler") || f.containsKey("session.save_handler")
                || f.containsKey("Session"),
                "php.ini should contain session settings");
        String timezone = (String) f.get("Date.date.timezone");
        if (timezone == null) {
            timezone = (String) f.get("date.timezone");
        }
        assertNotNull(timezone, "date.timezone should be present");
        assertTrue(timezone.contains("Seoul") || timezone.contains("Asia"),
                "Timezone should reference Asia/Seoul");
    }

    @Test
    void globalKeys() {
        ParseResult r = parseResource("samples/ini/global-keys.ini");
        assertHasBlock(r, "global-keys.ini");
        Map<String, Object> f = r.flattened();
        assertNotNull(r, "global-keys.ini should produce a result");
        assertFalse(r.blocks().isEmpty(), "global-keys.ini should produce at least one block");
        if (!f.isEmpty()) {
            assertTrue(f.containsKey("app_name") || f.containsKey("server.host")
                    || f.containsKey("server"),
                    "global-keys.ini should contain global or section keys");
        }
    }
}
