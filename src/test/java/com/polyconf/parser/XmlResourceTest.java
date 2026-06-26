package com.polyconf.parser;

import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmlResourceTest extends ResourceTestBase {

    @Test
    void config() {
        ParseResult r = parseResource("samples/xml/config.xml");
        assertNoErrors(r, "config.xml");
        assertFormat(r, Format.XML, "config.xml");
        Map<String, Object> f = r.flattened();
        assertEquals("0.0.0.0", f.get("config.server.host"));
        assertEquals("8080", f.get("config.server.port"));
        assertEquals("false", f.get("config.server.debug"));
        assertEquals("localhost", f.get("config.database.host"));
        assertEquals("5432", f.get("config.database.port"));
        assertEquals("myapp", f.get("config.database.name"));
        assertEquals("30000", f.get("config.features.timeout_ms"));
    }

    @Test
    void pom() {
        ParseResult r = parseResource("samples/xml/pom.xml");
        assertNoErrors(r, "pom.xml");
        assertFormat(r, Format.XML, "pom.xml");
        Map<String, Object> f = r.flattened();
        assertEquals("com.example", f.get("project.groupId"));
        assertEquals("my-app", f.get("project.artifactId"));
        assertEquals("1.0.0", f.get("project.version"));
        assertEquals("My Application", f.get("project.name"));
        assertEquals("17", f.get("project.properties.java.version"));
    }

    @Test
    void settings() {
        ParseResult r = parseResource("samples/xml/settings.xml");
        assertNoErrors(r, "settings.xml");
        assertFormat(r, Format.XML, "settings.xml");
        Map<String, Object> f = r.flattened();
        assertEquals("/home/user/.m2/repository", f.get("settings.localRepository"));
        assertEquals("central-mirror", f.get("settings.mirrors.mirror.id"));
        assertEquals("production", f.get("settings.activeProfiles.activeProfile"));
    }

    @Test
    void web() {
        ParseResult r = parseResource("samples/xml/web.xml");
        assertNoErrors(r, "web.xml");
        assertFormat(r, Format.XML, "web.xml");
        Map<String, Object> f = r.flattened();
        assertEquals("My Web Application", f.get("web-app.display-name"));
        assertEquals("30", f.get("web-app.session-config.session-timeout"));
        assertEquals("index.html", f.get("web-app.welcome-file-list.welcome-file[0]"));
    }

    @Test
    void logback() {
        ParseResult r = parseResource("samples/xml/logback.xml");
        assertHasBlock(r, "logback.xml");
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "logback.xml should produce non-empty flattened output");
        assertEquals("${LOG_PATH:-/var/log/myapp}", f.get("configuration.property[0].@value"));
        assertTrue(f.containsKey("configuration.logger[0].@name"),
                "logback.xml should contain logger definitions");
    }

    @Test
    void data() {
        ParseResult r = parseResource("samples/xml/data.xml");
        assertHasBlock(r, "data.xml");
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "data.xml should produce non-empty output");
        assertTrue(f.containsKey("configuration") || f.containsKey("configuration.database.host"),
                "data.xml should contain configuration");
    }
}
