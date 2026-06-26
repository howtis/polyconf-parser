package com.polyconf.parser;

import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DotenvResourceTest extends ResourceTestBase {

    @Test
    void env() {
        ParseResult r = parseResource("samples/dotenv/.env");
        assertNoErrors(r, ".env");
        assertFormat(r, Format.DOTENV, ".env");
        Map<String, Object> f = r.flattened();
        assertEquals("My Application", f.get("APP_NAME"));
        assertEquals("production", f.get("APP_ENV"));
        assertEquals("false", f.get("APP_DEBUG"));
        assertEquals("localhost", f.get("DATABASE_HOST"));
        assertEquals("5432", f.get("DATABASE_PORT"));
        assertEquals("myapp", f.get("DATABASE_NAME"));
        assertEquals("postgres://localhost:5432/myapp", f.get("DATABASE_URL"));
        assertEquals("s3cr3t-k3y-v4lu3", f.get("SECRET_KEY"));
        assertEquals("localhost,127.0.0.1,example.com", f.get("ALLOWED_HOSTS"));
        assertEquals("100", f.get("MAX_CONNECTIONS"));
        assertEquals("30", f.get("TIMEOUT"));
    }

    @Test
    void envProduction() {
        ParseResult r = parseResource("samples/dotenv/.env.production");
        assertTrue(r.hasErrors(), "Expected circular reference errors");
        assertFormat(r, Format.DOTENV, ".env.production");
        Map<String, Object> f = r.flattened();
        assertEquals("production", f.get("APP_ENV"));
        assertEquals("false", f.get("APP_DEBUG"));
        assertEquals("db-prod.internal", f.get("DB_HOST"));
        assertEquals("5432", f.get("DB_PORT"));
        assertEquals("redis-prod.internal", f.get("REDIS_HOST"));
        assertEquals("6379", f.get("REDIS_PORT"));
    }

    @Test
    void envTest() {
        ParseResult r = parseResource("samples/dotenv/.env.test");
        assertNoErrors(r, ".env.test");
        assertFormat(r, Format.DOTENV, ".env.test");
        Map<String, Object> f = r.flattened();
        assertEquals("test", f.get("APP_ENV"));
        assertEquals("true", f.get("APP_DEBUG"));
        assertEquals("3000", f.get("SERVER_PORT"));
        assertEquals("localhost", f.get("DB_HOST"));
        assertEquals("5432", f.get("DB_PORT"));
        assertEquals("localhost", f.get("REDIS_HOST"));
        assertEquals("6379", f.get("REDIS_PORT"));
        assertEquals("test-secret-key", f.get("JWT_SECRET"));
        assertEquals("10000", f.get("TEST_TIMEOUT"));
        assertEquals("true", f.get("MOCK_ENABLED"));
    }

    @Test
    void envDocker() {
        ParseResult r = parseResource("samples/dotenv/.env.docker");
        assertNoErrors(r, ".env.docker");
        assertFormat(r, Format.DOTENV, ".env.docker");
        Map<String, Object> f = r.flattened();
        assertEquals("orders-service", f.get("APP_NAME"));
        assertEquals("production", f.get("APP_ENV"));
        assertEquals("false", f.get("APP_DEBUG"));
        assertEquals("8080", f.get("APP_PORT"));
        assertEquals("postgres-primary.internal", f.get("DB_HOST"));
        assertEquals("5432", f.get("DB_PORT"));
        assertEquals("true", f.get("PROMETHEUS_ENABLED"));
    }
}
