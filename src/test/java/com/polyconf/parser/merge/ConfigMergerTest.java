package com.polyconf.parser.merge;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.model.ValueType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMergerTest {

    private final ConfigMerger merger = new ConfigMerger();

    private static ConfigValue val(String key, String value) {
        return new ConfigValue(key, value, ValueType.STRING,
                new Provenance(0, Format.PROPERTIES, key + "=" + value, 1.0), key);
    }

    private static ConfigSection section(String key, ConfigValue... children) {
        ConfigSection s = new ConfigSection(key, null, key);
        for (ConfigValue child : children) {
            s.children().put(child.key(), child);
        }
        return s;
    }

    private static ConfigSection section(String key, ConfigSection... children) {
        ConfigSection s = new ConfigSection(key, null, key);
        for (ConfigSection child : children) {
            s.children().put(child.key(), child);
        }
        return s;
    }

    private static ConfigSection section(String key, ConfigNode... children) {
        ConfigSection s = new ConfigSection(key, null, key);
        for (ConfigNode child : children) {
            if (child instanceof ConfigSection cs) {
                s.children().put(cs.key(), cs);
            } else if (child instanceof ConfigValue cv) {
                s.children().put(cv.key(), cv);
            }
        }
        return s;
    }

    @Test
    void mergeDisjointKeys() {
        ConfigSection block1 = section("", val("host", "localhost"));
        ConfigSection block2 = section("", val("port", "5432"));

        MergeResult result = merger.merge(List.of(block1, block2));

        assertEquals(2, result.merged().children().size());
        assertEquals("localhost", result.merged().childValue("host").orElseThrow().asString().orElseThrow());
        assertEquals("5432", result.merged().childValue("port").orElseThrow().asString().orElseThrow());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void collisionOverwrite() {
        ConfigSection block1 = section("", val("host", "oldhost"));
        ConfigSection block2 = section("", val("host", "newhost"));

        MergeResult result = merger.merge(List.of(block1, block2));

        assertEquals(1, result.merged().children().size());
        assertEquals("newhost", result.merged().childValue("host").orElseThrow().asString().orElseThrow());
    }

    @Test
    void mergeNestedSections() {
        ConfigSection block1 = section("",
                section("db", val("host", "localhost")));
        ConfigSection block2 = section("",
                section("db", val("port", "5432")));

        MergeResult result = merger.merge(List.of(block1, block2));

        ConfigSection dbSection = result.merged().childSection("db").orElseThrow();
        assertEquals(2, dbSection.children().size());
        assertEquals("localhost", dbSection.childValue("host").orElseThrow().asString().orElseThrow());
        assertEquals("5432", dbSection.childValue("port").orElseThrow().asString().orElseThrow());
    }

    @Test
    void nestedSectionValueCollision() {
        ConfigSection block3 = section("",
                section("db", val("host", "overwritten")));
        ConfigSection block1 = section("",
                section("db", val("host", "localhost")));
        ConfigSection block2 = section("",
                section("db", val("host", "newhost")));

        MergeResult result = merger.merge(List.of(block1, block2, block3));

        ConfigSection db = result.merged().childSection("db").orElseThrow();
        assertEquals("overwritten", db.childValue("host").orElseThrow().asString().orElseThrow());
    }

    @Test
    void collisionWarnMode() {
        ConfigSection block1 = section("", val("key", "first"));
        ConfigSection block2 = section("", val("key", "second"));

        MergePolicy policy = new MergePolicy(CollisionMode.WARN, TypeConflictMode.OVERWRITE);
        MergeResult result = merger.merge(List.of(block1, block2), policy);

        assertEquals("second", result.merged().childValue("key").orElseThrow().asString().orElseThrow());
        assertEquals(1, result.diagnostics().size());
        BlockDiagnostic diag = result.diagnostics().get(0);
        assertEquals(DiagnosticLevel.WARNING, diag.level());
        assertTrue(diag.message().contains("key"));
        assertTrue(diag.message().contains("Collision"));
    }

    @Test
    void collisionRejectMode() {
        ConfigSection block1 = section("", val("key", "first"));
        ConfigSection block2 = section("", val("key", "second"));

        MergePolicy policy = new MergePolicy(CollisionMode.REJECT, TypeConflictMode.OVERWRITE);
        assertThrows(MergeConflictException.class, () -> merger.merge(List.of(block1, block2), policy));
    }

    @Test
    void typeConflictDefaultWarn() {
        ConfigSection block1 = section("", val("db", "justastring"));
        ConfigSection block2 = section("", section("db", val("host", "localhost")));

        MergeResult result = merger.merge(List.of(block1, block2));

        assertNotNull(result.merged().childSection("db").orElse(null));
        assertEquals(1, result.diagnostics().size());
        BlockDiagnostic diag = result.diagnostics().get(0);
        assertTrue(diag.message().contains("Type conflict"));
        assertTrue(diag.message().contains("db"));
    }

    @Test
    void typeConflictRejectMode() {
        ConfigSection block1 = section("", val("db", "justastring"));
        ConfigSection block2 = section("", section("db", val("host", "localhost")));

        MergePolicy policy = new MergePolicy(CollisionMode.OVERWRITE, TypeConflictMode.REJECT);
        assertThrows(MergeConflictException.class, () -> merger.merge(List.of(block1, block2), policy));
    }

    @Test
    void emptyInput() {
        MergeResult result = merger.merge(List.of());
        assertTrue(result.merged().children().isEmpty());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void singleSection() {
        ConfigSection section = section("", val("key", "value"));
        MergeResult result = merger.merge(List.of(section));

        assertEquals(1, result.merged().children().size());
        assertEquals("value", result.merged().childValue("key").orElseThrow().asString().orElseThrow());
    }

    @Test
    void nullSectionInList() {
        ConfigSection block1 = section("", val("key", "value"));
        List<ConfigSection> sections = new ArrayList<>();
        sections.add(block1);
        sections.add(null);
        MergeResult result = merger.merge(sections);

        assertEquals(1, result.merged().children().size());
    }

    @Test
    void nullSectionsThrows() {
        assertThrows(IllegalArgumentException.class, () -> merger.merge(null));
    }

    @Test
    void nullPolicyThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> merger.merge(List.of(section("", val("k", "v"))), null));
    }

    @Test
    void threeWayMerge() {
        ConfigSection block1 = section("", val("a", "1"), val("b", "2"));
        ConfigSection block2 = section("", val("b", "overwritten"), val("c", "3"));
        ConfigSection block3 = section("", val("d", "4"));

        MergeResult result = merger.merge(List.of(block1, block2, block3));

        assertEquals(4, result.merged().children().size());
        assertEquals("1", result.merged().childValue("a").orElseThrow().asString().orElseThrow());
        assertEquals("overwritten", result.merged().childValue("b").orElseThrow().asString().orElseThrow());
        assertEquals("3", result.merged().childValue("c").orElseThrow().asString().orElseThrow());
        assertEquals("4", result.merged().childValue("d").orElseThrow().asString().orElseThrow());
    }

    @Test
    void deepNestedMerge() {
        ConfigSection block1 = section("",
                section("app", section("db",
                        val("host", "localhost"))));
        ConfigSection block2 = section("",
                section("app", section("db",
                        val("port", "5432"))));
        ConfigSection block3 = section("",
                section("app", val("name", "myapp")));

        MergeResult result = merger.merge(List.of(block1, block2, block3));

        ConfigSection app = result.merged().childSection("app").orElseThrow();
        assertEquals(2, app.children().size());

        ConfigSection db = app.childSection("db").orElseThrow();
        assertEquals("localhost", db.childValue("host").orElseThrow().asString().orElseThrow());
        assertEquals("5432", db.childValue("port").orElseThrow().asString().orElseThrow());

        assertEquals("myapp", app.childValue("name").orElseThrow().asString().orElseThrow());
    }
}
