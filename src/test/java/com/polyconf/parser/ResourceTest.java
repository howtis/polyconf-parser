package com.polyconf.parser;

import com.polyconf.parser.model.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResourceTest {

    private final PolyconfParser parser = new PolyconfParser();

    private List<String> readLines(String resourcePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(is, "Resource not found: " + resourcePath);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            fail("Failed to read resource: " + resourcePath, e);
        }
        return lines;
    }

    private ParseResult parseResource(String resourcePath) {
        return parser.parse(readLines(resourcePath));
    }

    private void assertNoErrors(ParseResult result, String description) {
        List<BlockDiagnostic> errors = result.diagnostics().stream()
                .filter(d -> d.level() == DiagnosticLevel.ERROR)
                .toList();
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Errors in " + description + ":");
            for (BlockDiagnostic d : errors) {
                sb.append("\n  L").append(d.startLine()).append("-L").append(d.endLine())
                        .append(": ").append(d.message());
            }
            fail(sb.toString());
        }
    }

    private void assertHasBlock(ParseResult result, String description) {
        assertFalse(result.blocks().isEmpty(),
                "Expected at least one block for " + description);
    }

    private void assertFormat(ParseResult result, Format expected, String description) {
        assertHasBlock(result, description);
        assertEquals(expected, result.blocks().get(0).detectedFormat(),
                "Wrong format detected for " + description);
    }

    // ---- JSON samples ----

    @Nested
    class JsonSamples {
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
    }

    // ---- JSON5 samples ----

    @Nested
    class Json5Samples {
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
            // Numeric formats
            assertEquals(42L, f.get("integer"));
            assertEquals(-17L, f.get("negative"));
            assertEquals(255L, f.get("hex"));
            assertEquals(3.14159, (Double) f.get("float"), 0.00001);
            assertEquals(Double.POSITIVE_INFINITY, f.get("infinity"));
            assertEquals(Double.NEGATIVE_INFINITY, f.get("neg_infinity"));
            // Strings
            assertEquals("hello world", f.get("single_quoted"));
            assertEquals("hello\nworld", f.get("double_quoted"));
            assertEquals("valid in JSON5", f.get("unquoted_key"));
            // Booleans and null
            assertEquals(true, f.get("enabled"));
            assertEquals(false, f.get("disabled"));
            assertNull(f.get("empty"));
            // Nested
            assertEquals(1L, f.get("nested.a"));
            assertEquals(2L, f.get("nested.b"));
            assertEquals(3L, f.get("nested.c"));
            // Array
            assertEquals("first", f.get("items[0]"));
            assertEquals("second", f.get("items[1]"));
            assertEquals("third", f.get("items[2]"));
        }
    }

    // ---- TOML samples ----

    @Nested
    class TomlSamples {
        @Test
        void simple() {
            ParseResult r = parseResource("samples/toml/simple.toml");
            assertNoErrors(r, "simple.toml");
            assertFormat(r, Format.TOML, "simple.toml");
            Map<String, Object> f = r.flattened();
            assertEquals("TOML Example", f.get("title"));
            assertEquals(1L, f.get("version"));
            assertEquals("John Doe", f.get("owner.name"));
            assertEquals("Software developer", f.get("owner.bio"));
            assertEquals("1.1.0", f.get("dependencies.tomlj"));
            assertEquals("2.2", f.get("dependencies.snakeyaml"));
        }

        @Test
        void server() {
            ParseResult r = parseResource("samples/toml/server.toml");
            System.out.println("=== TOML flat keys: " + r.flattened().keySet());
            assertNoErrors(r, "server.toml");
            assertFormat(r, Format.TOML, "server.toml");
            Map<String, Object> f = r.flattened();
            assertEquals("0.0.0.0", f.get("server.host"));
            assertEquals(8080L, f.get("server.port"));
            assertEquals(false, f.get("server.debug"));
            assertEquals("localhost", f.get("database.host"));
            assertEquals(5432L, f.get("database.port"));
            assertEquals("myapp", f.get("database.name"));
            assertEquals("admin", f.get("database.credentials.username"));
            assertEquals("a.example.com", f.get("servers[0].host"));
            assertEquals("b.example.com", f.get("servers[1].host"));
            assertEquals(1000L, f.get("features.rate_limit.max_requests"));
            assertEquals(60L, f.get("features.rate_limit.window_seconds"));
        }

        @Test
        void complex() {
            ParseResult r = parseResource("samples/toml/complex.toml");
            assertNoErrors(r, "complex.toml");
            assertFormat(r, Format.TOML, "complex.toml");
            Map<String, Object> f = r.flattened();
            assertEquals("Complex TOML", f.get("title"));
            assertEquals("Tom Preston-Werner", f.get("owner.name"));
            assertEquals(true, f.get("database.enabled"));
            assertEquals(8000L, f.get("database.ports[0]"));
            assertEquals(8001L, f.get("database.ports[1]"));
            assertEquals(8002L, f.get("database.ports[2]"));
            assertEquals("10.0.0.1", f.get("servers.alpha.ip"));
            assertEquals("frontend", f.get("servers.alpha.role"));
            assertEquals("10.0.0.2", f.get("servers.beta.ip"));
            assertEquals("backend", f.get("servers.beta.role"));
            assertEquals("Hammer", f.get("products[0].name"));
            assertEquals(738594937L, f.get("products[0].sku"));
            assertEquals("Nail", f.get("products[1].name"));
            assertEquals(284758393L, f.get("products[1].sku"));
        }
    }

    // ---- YAML samples ----

    @Nested
    class YamlSamples {
        @Test
        void simple() {
            ParseResult r = parseResource("samples/yaml/simple.yaml");
            assertNoErrors(r, "simple.yaml");
            assertFormat(r, Format.YAML, "simple.yaml");
            Map<String, Object> f = r.flattened();
            assertEquals("simple-yaml", f.get("name"));
            assertEquals(1L, f.get("version"));
            assertEquals("Jane Smith", f.get("author"));
        }

        @Test
        void server() {
            ParseResult r = parseResource("samples/yaml/server.yaml");
            assertNoErrors(r, "server.yaml");
            assertFormat(r, Format.YAML, "server.yaml");
            Map<String, Object> f = r.flattened();
            assertEquals("0.0.0.0", f.get("server.host"));
            assertEquals(8080L, f.get("server.port"));
            assertEquals(false, f.get("server.debug"));
            assertEquals("localhost", f.get("database.host"));
            assertEquals(5432L, f.get("database.port"));
            assertEquals("myapp", f.get("database.name"));
            assertEquals("admin", f.get("database.credentials.username"));
            assertEquals("secret", f.get("database.credentials.password"));
            assertEquals("a.example.com", f.get("servers[0].host"));
            assertEquals("b.example.com", f.get("servers[1].host"));
            assertEquals(3.14, (Double) f.get("features.pi"), 0.001);
            assertEquals(1000L, f.get("features.rate_limit.max_requests"));
        }

        @Test
        void complex() {
            ParseResult r = parseResource("samples/yaml/complex.yaml");
            assertNoErrors(r, "complex.yaml");
            assertFormat(r, Format.YAML, "complex.yaml");
            Map<String, Object> f = r.flattened();
            assertEquals("myapp", f.get("app.name"));
            assertEquals("2.1.0", f.get("app.version"));
            assertEquals("production", f.get("app.environment"));
            assertEquals(8080L, f.get("server.port"));
            assertEquals(true, f.get("server.ssl.enabled"));
            assertEquals(4L, f.get("server.workers"));
            assertEquals("db-primary.example.com", f.get("database.primary.host"));
            assertEquals(5432L, f.get("database.primary.port"));
            assertEquals(5L, f.get("database.primary.pool.min"));
            assertEquals(20L, f.get("database.primary.pool.max"));
            assertEquals("db-replica.example.com", f.get("database.replica.host"));
            assertEquals("redis.example.com", f.get("cache.redis.host"));
            assertEquals(6379L, f.get("cache.redis.port"));
            assertEquals(true, f.get("features.auth.enabled"));
            assertTrue(((String) f.get("features.auth.providers[0]")).contains("google"));
            assertEquals("info", f.get("features.logging.level"));
            assertEquals(true, f.get("features.metrics.enabled"));
            assertEquals(true, f.get("rate_limit.enabled"));
            assertEquals(100L, f.get("rate_limit.default.requests"));
            assertEquals("smtp.example.com", f.get("notifications.email.smtp.host"));
            assertEquals(587L, f.get("notifications.email.smtp.port"));
        }

        @Test
        void dockerCompose() {
            ParseResult r = parseResource("samples/yaml/docker-compose.yaml");
            assertNoErrors(r, "docker-compose.yaml");
            assertFormat(r, Format.YAML, "docker-compose.yaml");
            Map<String, Object> f = r.flattened();
            assertEquals("3.8", f.get("version"));
            assertEquals("nginx:alpine", f.get("services.web.image"));
            assertEquals("80:80", f.get("services.web.ports[0]"));
            assertEquals("8080:8080", f.get("services.api.ports[0]"));
            assertEquals("postgres:16-alpine", f.get("services.db.image"));
            assertEquals("redis:7-alpine", f.get("services.redis.image"));
        }
    }

    // ---- XML samples ----

    @Nested
    class XmlSamples {
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
    }

    // ---- INI samples ----

    @Nested
    class IniSamples {
        @Test
        void config() {
            ParseResult r = parseResource("samples/ini/config.ini");
            assertNoErrors(r, "config.ini");
            assertFormat(r, Format.INI, "config.ini");
            Map<String, Object> f = r.flattened();
            assertEquals("0.0.0.0", f.get("server.host"));
            assertEquals(8080L, f.get("server.port"));
            assertEquals(false, f.get("server.debug"));
            assertEquals("localhost", f.get("database.host"));
            assertEquals(5432L, f.get("database.port"));
            assertEquals("myapp", f.get("database.name"));
            assertEquals(true, f.get("features.auth"));
            assertEquals(true, f.get("features.logging"));
            assertEquals(true, f.get("features.metrics"));
            assertEquals(30000L, f.get("features.timeout_ms"));
        }

        @Test
        void database() {
            ParseResult r = parseResource("samples/ini/database.ini");
            assertNoErrors(r, "database.ini");
            assertFormat(r, Format.INI, "database.ini");
            Map<String, Object> f = r.flattened();
            assertEquals("db.example.com", f.get("client.host"));
            assertEquals(3306L, f.get("client.port"));
            assertEquals("myapp", f.get("client.database"));
            assertEquals("app_user", f.get("client.user"));
            assertEquals("utf8mb4", f.get("client.charset"));
            assertEquals("/etc/ssl/ca-cert.pem", f.get("client.ssl.ca"));
            assertEquals("0.0.0.0", f.get("server.bind-address"));
            assertEquals(200L, f.get("server.max_connections"));
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
            assertEquals(4L, f.get("Game.max_players"));
            assertEquals("normal", f.get("Game.difficulty"));
            assertEquals("1920x1080", f.get("Graphics.resolution"));
            assertEquals(true, f.get("Graphics.fullscreen"));
            assertEquals(true, f.get("Graphics.vsync"));
            assertEquals(0.8, (Double) f.get("Audio.master_volume"), 0.01);
            assertEquals(0.6, (Double) f.get("Audio.music_volume"), 0.01);
            assertEquals(1.0, (Double) f.get("Audio.sfx_volume"), 0.01);
        }
    }

    // ---- Properties samples ----

    @Nested
    class PropertiesSamples {
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
    }

    // ---- Dotenv samples ----

    @Nested
    class DotenvSamples {
        @Test
        void env() {
            ParseResult r = parseResource("samples/dotenv/.env");
            assertNoErrors(r, ".env");
            assertFormat(r, Format.DOTENV, ".env");
            Map<String, Object> f = r.flattened();
            assertEquals("myapp", f.get("APP_NAME"));
            assertEquals("production", f.get("APP_ENV"));
            assertEquals(true, f.get("APP_DEBUG"));
            assertEquals(8080L, f.get("APP_PORT"));
            assertEquals("0.0.0.0", f.get("APP_HOST"));
            assertEquals("localhost", f.get("DB_HOST"));
            assertEquals(5432L, f.get("DB_PORT"));
            assertEquals("localhost:6379", f.get("REDIS_URL"));
        }

        @Test
        void envProduction() {
            ParseResult r = parseResource("samples/dotenv/.env.production");
            assertNoErrors(r, ".env.production");
            assertFormat(r, Format.DOTENV, ".env.production");
            Map<String, Object> f = r.flattened();
            assertEquals("myapp-prod", f.get("APP_NAME"));
            assertEquals("production", f.get("APP_ENV"));
            assertEquals(false, f.get("APP_DEBUG"));
            assertEquals("db-prod.internal", f.get("DB_HOST"));
            assertEquals("redis-prod.internal", f.get("REDIS_HOST"));
            assertEquals("prod-secret-key", f.get("SECRET_KEY"));
        }

        @Test
        void envTest() {
            ParseResult r = parseResource("samples/dotenv/.env.test");
            assertNoErrors(r, ".env.test");
            assertFormat(r, Format.DOTENV, ".env.test");
            Map<String, Object> f = r.flattened();
            assertEquals("myapp-test", f.get("APP_NAME"));
            assertEquals("testing", f.get("APP_ENV"));
            assertEquals(true, f.get("APP_DEBUG"));
            assertEquals("localhost", f.get("DB_HOST"));
            assertEquals("localhost", f.get("REDIS_HOST"));
            assertEquals("test-secret-key", f.get("SECRET_KEY"));
        }
    }

    // ---- HOCON samples ----

    @Nested
    class HoconSamples {
        @Test
        void app() {
            ParseResult r = parseResource("samples/hocon/app.hocon");
            assertNoErrors(r, "app.hocon");
            assertFormat(r, Format.HOCON, "app.hocon");
            Map<String, Object> f = r.flattened();
            assertEquals("myapp", f.get("app.name"));
            assertEquals("1.0.0", f.get("app.version"));
            assertEquals("A polyglot configuration parser", f.get("app.description"));
            assertEquals("0.0.0.0", f.get("server.host"));
            assertEquals(8080L, f.get("server.port"));
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
            assertEquals(30000L, f.get("features.timeout_ms"));
            assertEquals(1000L, f.get("features.rate_limit.max-requests"));
            assertEquals(60L, f.get("features.rate_limit.window-seconds"));
        }
    }

    // ---- KDL samples ----

    @Nested
    class KdlSamples {
        @Test
        void app() {
            ParseResult r = parseResource("samples/kdl/app.kdl");
            assertNoErrors(r, "app.kdl");
            assertFormat(r, Format.KDL, "app.kdl");
            Map<String, Object> f = r.flattened();
            assertTrue(f.containsKey("server"), "Should contain server node");
            assertTrue(f.containsKey("database"), "Should contain database node");
        }

        @Test
        void complex() {
            ParseResult r = parseResource("samples/kdl/complex.kdl");
            assertNoErrors(r, "complex.kdl");
            assertFormat(r, Format.KDL, "complex.kdl");
            Map<String, Object> f = r.flattened();
            assertTrue(f.containsKey("package"), "Should contain package node");
            assertTrue(f.containsKey("infrastructure"), "Should contain infrastructure node");
            assertTrue(f.containsKey("settings"), "Should contain settings node");
        }

        @Test
        void config() {
            ParseResult r = parseResource("samples/kdl/config.kdl");
            assertNoErrors(r, "config.kdl");
            assertFormat(r, Format.KDL, "config.kdl");
            Map<String, Object> f = r.flattened();
            assertTrue(f.containsKey("title"), "Should contain title");
            assertTrue(f.containsKey("services"), "Should contain services node");
            assertTrue(f.containsKey("numbers"), "Should contain numbers array");
            assertTrue(f.containsKey("names"), "Should contain names array");
            assertTrue(f.containsKey("count"), "Should contain count (type annotation)");
            assertTrue(f.containsKey("ratio"), "Should contain ratio (type annotation)");
        }
    }

    // ---- Edge case files ----

    @Nested
    class EdgeCaseFiles {
        @Test
        void adjacentMixedNoBlankline() {
            ParseResult r = parseResource("edge-cases/adjacent-mixed-no-blankline.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void ambiguousJsonlike() {
            ParseResult r = parseResource("edge-cases/ambiguous-jsonlike.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void ambiguousKeyvalue() {
            ParseResult r = parseResource("edge-cases/ambiguous-keyvalue.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void closeScoreTie() {
            ParseResult r = parseResource("edge-cases/close-score-tie.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void deeplyNested() {
            ParseResult r = parseResource("edge-cases/deeply-nested.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void duplicateKeys() {
            ParseResult r = parseResource("edge-cases/duplicate-keys.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
            assertTrue(r.flattened().containsKey("key"));
        }

        @Test
        void empty() {
            ParseResult r = parseResource("edge-cases/empty.txt");
            assertNotNull(r);
            assertFalse(r.hasErrors());
        }

        @Test
        void formatScoringGaps() {
            ParseResult r = parseResource("edge-cases/format-scoring-gaps.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void hintWrongFormat() {
            ParseResult r = parseResource("edge-cases/hint-wrong-format.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void jsonWithComments() {
            ParseResult r = parseResource("edge-cases/json-with-comments.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void leadingZeros() {
            ParseResult r = parseResource("edge-cases/leading-zeros.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void multilineStrings() {
            ParseResult r = parseResource("edge-cases/multiline-strings.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void numericEdgeCases() {
            ParseResult r = parseResource("edge-cases/numeric-edge-cases.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void propertiesTypeFlat() {
            ParseResult r = parseResource("edge-cases/properties-type-flat.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void tomlTableArrayVsIni() {
            ParseResult r = parseResource("edge-cases/toml-table-array-vs-ini.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void unicodeI18n() {
            ParseResult r = parseResource("edge-cases/unicode-i18n.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void weirdContent() {
            ParseResult r = parseResource("edge-cases/weird-content.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }

        @Test
        void weirdKeys() {
            ParseResult r = parseResource("edge-cases/weird-keys.txt");
            assertNotNull(r);
            assertNotNull(r.flattened());
        }
    }
}
