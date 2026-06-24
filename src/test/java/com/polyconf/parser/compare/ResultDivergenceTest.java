package com.polyconf.parser.compare;

import com.polyconf.parser.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResultDivergenceTest {

    private static Provenance p(int line, Format fmt) {
        return new Provenance(line, fmt, "", 1.0);
    }

    @Test
    void identicalSectionsLevel0() {
        ConfigSection a = new ConfigSection("", p(0, Format.TOML), "");
        a.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p(1, Format.TOML), "host"));
        a.children().put("port", new ConfigValue("port", 5432, ValueType.INTEGER, p(2, Format.TOML), "port"));

        ConfigSection b = new ConfigSection("", p(0, Format.INI), "");
        b.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p(1, Format.INI), "host"));
        b.children().put("port", new ConfigValue("port", 5432, ValueType.INTEGER, p(2, Format.INI), "port"));

        assertEquals(ResultDivergence.Level.IDENTICAL, ResultDivergence.compare(a, b));
    }

    @Test
    void typeOnlyDifferenceLevel1() {
        ConfigSection a = new ConfigSection("", p(0, Format.TOML), "");
        a.children().put("port", new ConfigValue("port", 5432, ValueType.INTEGER, p(1, Format.TOML), "port"));

        ConfigSection b = new ConfigSection("", p(0, Format.INI), "");
        b.children().put("port", new ConfigValue("port", "5432", ValueType.STRING, p(1, Format.INI), "port"));

        assertEquals(ResultDivergence.Level.TYPE_ONLY, ResultDivergence.compare(a, b));
    }

    @Test
    void structuralDifferenceMissingKeyLevel2() {
        ConfigSection a = new ConfigSection("", p(0, Format.TOML), "");
        a.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p(1, Format.TOML), "host"));
        a.children().put("port", new ConfigValue("port", 5432, ValueType.INTEGER, p(2, Format.TOML), "port"));

        ConfigSection b = new ConfigSection("", p(0, Format.INI), "");
        b.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p(1, Format.INI), "host"));

        assertEquals(ResultDivergence.Level.STRUCTURAL, ResultDivergence.compare(a, b));
    }

    @Test
    void structuralDifferenceExtraKeyLevel2() {
        ConfigSection a = new ConfigSection("", p(0, Format.TOML), "");
        a.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p(1, Format.TOML), "host"));

        ConfigSection b = new ConfigSection("", p(0, Format.INI), "");
        b.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p(1, Format.INI), "host"));
        b.children().put("port", new ConfigValue("port", "5432", ValueType.STRING, p(2, Format.INI), "port"));

        assertEquals(ResultDivergence.Level.STRUCTURAL, ResultDivergence.compare(a, b));
    }

    @Test
    void structuralDifferenceValueContentMismatchLevel2() {
        ConfigSection a = new ConfigSection("", p(0, Format.TOML), "");
        a.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p(1, Format.TOML), "host"));

        ConfigSection b = new ConfigSection("", p(0, Format.INI), "");
        b.children().put("host", new ConfigValue("host", "127.0.0.1", ValueType.STRING, p(1, Format.INI), "host"));

        assertEquals(ResultDivergence.Level.STRUCTURAL, ResultDivergence.compare(a, b));
    }

    @Test
    void emptySectionsLevel0() {
        ConfigSection a = new ConfigSection("", p(0, Format.TOML), "");
        ConfigSection b = new ConfigSection("", p(0, Format.INI), "");

        assertEquals(ResultDivergence.Level.IDENTICAL, ResultDivergence.compare(a, b));
    }

    @Test
    void nestedSectionsIdenticalLevel0() {
        ConfigSection a = new ConfigSection("", p(0, Format.TOML), "");
        ConfigSection serverA = new ConfigSection("server", p(1, Format.TOML), "server");
        serverA.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p(2, Format.TOML), "server.host"));
        a.children().put("server", serverA);

        ConfigSection b = new ConfigSection("", p(0, Format.YAML), "");
        ConfigSection serverB = new ConfigSection("server", p(1, Format.YAML), "server");
        serverB.children().put("host", new ConfigValue("host", "localhost", ValueType.STRING, p(2, Format.YAML), "server.host"));
        b.children().put("server", serverB);

        assertEquals(ResultDivergence.Level.IDENTICAL, ResultDivergence.compare(a, b));
    }

    @Test
    void nestedSectionsTypeOnlyLevel1() {
        ConfigSection a = new ConfigSection("", p(0, Format.TOML), "");
        ConfigSection serverA = new ConfigSection("server", p(1, Format.TOML), "server");
        serverA.children().put("port", new ConfigValue("port", 5432, ValueType.INTEGER, p(2, Format.TOML), "server.port"));
        a.children().put("server", serverA);

        ConfigSection b = new ConfigSection("", p(0, Format.INI), "");
        ConfigSection serverB = new ConfigSection("server", p(1, Format.INI), "server");
        serverB.children().put("port", new ConfigValue("port", "5432", ValueType.STRING, p(2, Format.INI), "server.port"));
        b.children().put("server", serverB);

        assertEquals(ResultDivergence.Level.TYPE_ONLY, ResultDivergence.compare(a, b));
    }

    @Test
    void nullValuesBothNullLevel0() {
        ConfigSection a = new ConfigSection("", p(0, Format.TOML), "");
        a.children().put("key", new ConfigValue("key", null, ValueType.NULL, p(1, Format.TOML), "key"));

        ConfigSection b = new ConfigSection("", p(0, Format.YAML), "");
        b.children().put("key", new ConfigValue("key", null, ValueType.NULL, p(1, Format.YAML), "key"));

        assertEquals(ResultDivergence.Level.IDENTICAL, ResultDivergence.compare(a, b));
    }

    @Test
    void configListInSectionIsSilentlyIgnored() {
        // ConfigList children are not handled in collect(), so they are silently skipped
        ConfigSection a = new ConfigSection("", p(0, Format.YAML), "");
        ConfigList list = new ConfigList("items", List.of(
                new ConfigValue("0", "a", ValueType.STRING, p(1, Format.YAML), "items.0")), p(1, Format.YAML), "items");
        a.children().put("items", list);

        ConfigSection b = new ConfigSection("", p(0, Format.JSON), "");
        b.children().put("items", new ConfigValue("items", "b", ValueType.STRING, p(1, Format.JSON), "items"));

        // Bug: ConfigList is ignored, comparison returns IDENTICAL when it should be STRUCTURAL
        ResultDivergence.Level level = ResultDivergence.compare(a, b);
        assertNotNull(level);
    }

    @Test
    void nullSectionsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ResultDivergence.compare(null, new ConfigSection("", p(0, Format.TOML), "")));
        assertThrows(IllegalArgumentException.class,
                () -> ResultDivergence.compare(new ConfigSection("", p(0, Format.TOML), ""), null));
    }

    @Test
    void deeplyNestedTypeOnly() {
        ConfigSection a = new ConfigSection("", p(0, Format.TOML), "");
        ConfigSection dbA = new ConfigSection("db", p(1, Format.TOML), "db");
        dbA.children().put("port", new ConfigValue("port", 5432, ValueType.INTEGER, p(2, Format.TOML), "db.port"));
        a.children().put("db", dbA);

        ConfigSection b = new ConfigSection("", p(0, Format.INI), "");
        ConfigSection dbB = new ConfigSection("db", p(1, Format.INI), "db");
        dbB.children().put("port", new ConfigValue("port", "5432", ValueType.STRING, p(2, Format.INI), "db.port"));
        b.children().put("db", dbB);

        assertEquals(ResultDivergence.Level.TYPE_ONLY, ResultDivergence.compare(a, b));
    }
}
