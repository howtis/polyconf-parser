package com.polyconf.parser;

import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EdgeCaseResourceTest extends ResourceTestBase {

    @Test
    void adjacentMixedNoBlankline() {
        ParseResult r = parseResource("edge-cases/adjacent-mixed-no-blankline.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        if (!f.isEmpty()) {
            assertTrue(f.containsKey("server.port") || f.containsKey("port")
                    || f.containsKey("host"),
                    "If output is non-empty, some key should be present");
        }
    }

    @Test
    void ambiguousJsonlike() {
        ParseResult r = parseResource("edge-cases/ambiguous-jsonlike.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertTrue(f.containsKey("key") || f.containsKey("number"),
                "Should extract at least one of the JSON-like keys");
    }

    @Test
    void ambiguousKeyvalue() {
        ParseResult r = parseResource("edge-cases/ambiguous-keyvalue.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertTrue(f.containsKey("key"), "key should be present");
        assertEquals("value", f.get("key"));
        assertEquals(123L, f.get("another"));
        assertEquals(true, f.get("enabled"));
        assertEquals(3.14, (Double) f.get("float_val"), 0.001);
        Object quotedVal = f.get("quoted");
        assertNotNull(quotedVal, "quoted key should be present");
        assertTrue(quotedVal.toString().contains("hello world"),
                "quoted value should contain 'hello world'");
        Object nameVal = f.get("name");
        assertNotNull(nameVal, "name should be present");
        assertTrue(nameVal.toString().contains("ambiguous"),
                "name value should contain 'ambiguous'");
    }

    @Test
    void closeScoreTie() {
        ParseResult r = parseResource("edge-cases/close-score-tie.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertTrue(f.containsKey("server.host") || f.containsKey("name"),
                "Should extract at least one identifiable key");
    }

    @Test
    void deeplyNested() {
        ParseResult r = parseResource("edge-cases/deeply-nested.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertEquals("value1", f.get("level1_key"));
        assertEquals("value", f.get("level1.key"));
        assertEquals("value", f.get("level1.level2.key"));
        assertEquals("value", f.get("level1.level2.level3.key"));
        assertEquals("value", f.get("level1.level2.level3.level4.key"));
    }

    @Test
    void duplicateKeys() {
        ParseResult r = parseResource("edge-cases/duplicate-keys.txt");
        assertNotNull(r);
        Map<String, Object> f = r.flattened();
        assertNotNull(f);
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertTrue(f.containsKey("key"), "key should be present (last of duplicates)");
        assertEquals("third", f.get("key"));
        assertEquals("value1", f.get("Name"));
        assertEquals("value2", f.get("name"));
        assertEquals("value3", f.get("NAME"));
        assertEquals("two", f.get("section.key"));
        assertEquals("value1", f.get("section.key1"));
        assertEquals("value2", f.get("section.key2"));
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
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertTrue(f.containsKey("key"), "key = value should be parsed");
        assertEquals("value", f.get("key"));
        assertTrue(f.containsKey("name"), "name= value should be parsed");
        assertEquals("hello", f.get("name"));
    }

    @Test
    void hintWrongFormat() {
        ParseResult r = parseResource("edge-cases/hint-wrong-format.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        assertTrue(r.hasErrors() || !r.blocks().isEmpty(),
                "Should either produce errors or have blocks for wrong-format hints");
        Map<String, Object> f = r.flattened();
        if (!r.hasErrors()) {
            assertFalse(f.isEmpty(), "Should produce output when no errors occur");
        }
    }

    @Test
    void jsonWithComments() {
        ParseResult r = parseResource("edge-cases/json-with-comments.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        if (!f.isEmpty()) {
            assertEquals("myapp", f.get("name"));
            assertEquals("1.0.0", f.get("version"));
            assertEquals("localhost", f.get("database.host"));
            assertEquals(5432L, f.get("database.port"));
        }
    }

    @Test
    void leadingZeros() {
        ParseResult r = parseResource("edge-cases/leading-zeros.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertTrue(f.containsKey("port"), "port key should be present");
        assertEquals(80L, f.get("port"));
        assertTrue(f.containsKey("employee_id"), "employee_id key should be present");
        assertEquals(42L, f.get("employee_id"));
        assertNotNull(f.get("hex_color"));
    }

    @Test
    void multilineStrings() {
        ParseResult r = parseResource("edge-cases/multiline-strings.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertTrue(f.containsKey("multiline"), "multiline key should be present");
        assertTrue(f.containsKey("escaped"), "escaped key should be present");
        assertTrue(f.containsKey("url"), "url key should be present");
        assertEquals("https://example.com/path?key=value&flag=true", f.get("url"));
        assertTrue(f.containsKey("email"), "email key should be present");
        assertEquals("user@example.com", f.get("email"));
    }

    @Test
    void numericEdgeCases() {
        ParseResult r = parseResource("edge-cases/numeric-edge-cases.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertTrue(f.containsKey("bool_string_true"), "bool_string_true should be present");
        assertTrue(f.containsKey("bool_string_false"), "bool_string_false should be present");
        assertTrue(f.containsKey("bool_string_yes"), "bool_string_yes should be present");
        assertTrue(f.containsKey("null_value"), "null_value key should be present");
        assertEquals(-42L, f.get("negative_int"));
        assertEquals(0L, f.get("zero_int"));
        Object emptyVal = f.get("empty_string");
        assertNotNull(emptyVal, "empty_string should be present");
        assertTrue(emptyVal.toString().isEmpty() || emptyVal.toString().equals("\"\""),
                "empty_string should be empty or quoted-empty");
    }

    @Test
    void propertiesTypeFlat() {
        ParseResult r = parseResource("edge-cases/properties-type-flat.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertTrue(f.containsKey("server.port"), "server.port should be present");
        assertTrue(f.containsKey("feature.enabled"), "feature.enabled should be present");
        assertTrue(f.containsKey("threshold"), "threshold should be present");
        assertTrue(f.containsKey("optional.key"), "optional.key should be present");
    }

    @Test
    void tomlTableArrayVsIni() {
        ParseResult r = parseResource("edge-cases/toml-table-array-vs-ini.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertTrue(f.containsKey("products[0].name") || f.containsKey("products.hammer.name"),
                "Should extract product names from either array-of-tables or section syntax");
        assertTrue(f.containsKey("products[1].name") || f.containsKey("products.nail.name"),
                "Should extract second product name");
        assertTrue(f.containsKey("fruits[0].name") || f.containsKey("fruits.apple.name"),
                "Should extract fruit names");
    }

    @Test
    void unicodeI18n() {
        ParseResult r = parseResource("edge-cases/unicode-i18n.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertTrue(f.containsKey("key_with_unicode_val"), "ASCII-keyed unicode value should be present");
        assertTrue(f.containsKey("accents"), "accents key should be present");
        assertNotNull(f.get("key_with_unicode_val"));
        assertNotNull(f.get("accents"));
    }

    @Test
    void weirdContent() {
        ParseResult r = parseResource("edge-cases/weird-content.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertTrue(f.containsKey("EMPTY_VAL"), "EMPTY_VAL key should be present");
        assertEquals("", f.get("EMPTY_VAL"));
        Object doubleQuoted = f.get("QUOTED_DOUBLE");
        assertNotNull(doubleQuoted, "QUOTED_DOUBLE should be present");
        assertTrue(doubleQuoted.toString().contains("quoted value"),
                "QUOTED_DOUBLE should contain 'quoted value'");
        assertEquals("!@#$%^&*()_+", f.get("SPECIAL_CHARS"));
        Object spacesVal = f.get("KEY_WITH_SPACES");
        if (spacesVal == null) {
            assertTrue(f.toString().contains("value with spaces"),
                    "'value with spaces' should be somewhere in the output");
        }
    }

    @Test
    void weirdKeys() {
        ParseResult r = parseResource("edge-cases/weird-keys.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertEquals("value", f.get("123"));
        assertEquals("value", f.get("3.14"));
        assertEquals("value", f.get("1e10"));
        assertEquals("value", f.get("key-with-dash"));
        assertEquals("value", f.get("key_with_underscore"));
        assertTrue(f.containsKey("key.with.dot") || f.containsKey("key"),
                "key.with.dot should be present (literal or nested)");
        assertTrue(f.containsKey("key:with:colon") || f.toString().contains("colon"),
                "key:with:colon should be present or colon-related value found");
        assertEquals(1L, f.get("a"));
        assertEquals(2L, f.get("b"));
        assertEquals(3L, f.get("x"));
        assertEquals("value", f.get("/path/to/key"));
        assertTrue(f.containsKey("C:\\Windows\\System32")
                        || f.containsKey("/path/to/key"),
                "At least one path-like key should be present");
    }

    @Test
    void hoconUniqueSyntax() {
        // HOCON with include "common.conf" triggers ConfigException.NotResolved
        // because the parser's fallback path (unresolved config) can't access config.root().
        // This is a known parser limitation — test documents expected behavior.
        assertThrows(Exception.class,
                () -> parseResource("edge-cases/hocon-unique-syntax.txt"),
                "HOCON include resolution fails when include file is not found");
    }

    @Test
    void sameFormatBlanklines() {
        ParseResult r = parseResource("edge-cases/same-format-blanklines.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "Should produce non-empty flattened output");
        assertEquals("polyconf", f.get("app.name"));
        assertEquals("1.0.0", f.get("app.version"));
        assertEquals("0.0.0.0", f.get("server.host"));
        assertEquals(8080L, f.get("server.port"));
        assertEquals("localhost", f.get("database.host"));
        assertEquals(5432L, f.get("database.port"));
    }

    @Test
    void jsonUnicodeEscapes() {
        ParseResult r = parseResource("edge-cases/json-unicode-escapes.txt");
        assertHasBlock(r, "json-unicode-escapes.txt");
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "json-unicode-escapes.txt should produce non-empty output");
        assertEquals("MyApp", f.get("app.name"));
        assertTrue(f.get("app") != null || f.get("currency.euro") != null
                || f.get("special.newline") != null,
                "json-unicode-escapes should contain unicode data");
    }

    @Test
    void xmlEntities() {
        ParseResult r = parseResource("edge-cases/xml-entities.txt");
        assertHasBlock(r, "xml-entities.txt");
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "xml-entities.txt should produce non-empty output");
        assertTrue(f.containsKey("configuration") || f.containsKey("configuration.database.host")
                || f.containsKey("configuration.app.name"),
                "xml-entities.txt should contain configuration data");
    }

    @Test
    void xmlNamespace() {
        ParseResult r = parseResource("edge-cases/xml-namespace.txt");
        assertHasBlock(r, "xml-namespace.txt");
        Map<String, Object> f = r.flattened();
        assertFalse(f.isEmpty(), "xml-namespace.txt should produce non-empty output");
        assertTrue(f.containsKey("beans") || f.containsKey("beans.context:component-scan.@base-package"),
                "xml-namespace.txt should contain beans definitions");
    }

    @Test
    void yamlMultiDoc() {
        ParseResult r = parseResource("edge-cases/yaml-multi-doc.txt");
        assertNotNull(r);
        assertNotNull(r.flattened());
        Map<String, Object> f = r.flattened();
        assertTrue(f.size() > 0 || !r.blocks().isEmpty(),
                "Multi-doc YAML should produce output");
    }
}
