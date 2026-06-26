package com.polyconf.parser;

import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlResourceTest extends ResourceTestBase {

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

    @Test
    void githubActions() {
        ParseResult r = parseResource("samples/yaml/github-actions.yaml");
        assertHasBlock(r, "github-actions.yaml");
        Map<String, Object> f = r.flattened();
        assertEquals("CI Pipeline", f.get("name"));
        assertFalse(f.isEmpty(), "github-actions.yaml should produce non-empty flattened output");
        assertEquals("build", f.get("docker.needs"));
        assertEquals("ubuntu-latest", f.get("docker.runs-on"));
        assertEquals("actions/checkout@v4", f.get("docker.steps[0].uses"));
        assertTrue(f.containsKey("root[0].name"), "docker steps should be present");
    }

    @Test
    void kubernetesDeployment() {
        ParseResult r = parseResource("samples/yaml/kubernetes-deployment.yaml");
        assertNoErrors(r, "kubernetes-deployment.yaml");
        assertFormat(r, Format.YAML, "kubernetes-deployment.yaml");
        Map<String, Object> f = r.flattened();
        assertEquals("apps/v1", f.get("apiVersion"));
        assertEquals("Deployment", f.get("kind"));
        assertEquals("nginx-deployment", f.get("metadata.name"));
        assertEquals("production", f.get("metadata.namespace"));
        assertEquals(3L, f.get("spec.replicas"));
        assertEquals("RollingUpdate", f.get("spec.strategy.type"));
        assertEquals("nginx:1.25-alpine", f.get("spec.template.spec.containers[0].image"));
    }

    @Test
    void springBoot() {
        ParseResult r = parseResource("samples/yaml/spring-boot.yaml");
        assertNoErrors(r, "spring-boot.yaml");
        assertFormat(r, Format.YAML, "spring-boot.yaml");
        Map<String, Object> f = r.flattened();
        assertEquals("order-service", f.get("spring.application.name"));
        assertEquals(20L, f.get("spring.datasource.hikari.maximum-pool-size"));
        assertEquals(5L, f.get("spring.datasource.hikari.minimum-idle"));
        assertEquals("validate", f.get("spring.jpa.hibernate.ddl-auto"));
        assertEquals("redis", f.get("spring.cache.type"));
        assertEquals("INFO", f.get("logging.level.root"));
    }

    @Test
    void anchors() {
        ParseResult r = parseResource("samples/yaml/anchors.yaml");
        assertHasBlock(r, "anchors.yaml");
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "anchors.yaml should produce non-empty output");
        assertTrue(f.containsKey("services") || f.containsKey("services.api.host")
                || f.containsKey("defaults"),
                "anchors.yaml should contain services or defaults");
    }

    @Test
    void multiline() {
        ParseResult r = parseResource("samples/yaml/multiline.yaml");
        assertHasBlock(r, "multiline.yaml");
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "multiline.yaml should produce non-empty output");
        assertEquals("Multi-line String Examples", f.get("title"));
        assertTrue(f.get("literal_script") != null || f.containsKey("literal_script"),
                "literal_script should be present");
    }
}
