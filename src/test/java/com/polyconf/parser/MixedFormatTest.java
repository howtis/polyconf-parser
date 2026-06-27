package com.polyconf.parser;

import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for block-level sub-segmentation: when two different formats appear on
 * adjacent lines without a blank line separator, the parser detects the format
 * boundary and splits the block into sub-blocks.
 */
class MixedFormatTest extends ResourceTestBase {

    /**
     * Properties key=value lines followed by XML adjacent (no blank line separator).
     * Should produce 2 sub-blocks: PROPERTIES and XML.
     */
    @Test
    void mixedFormatPropertiesXml() {
        ParseResult r = parseResource("edge-cases/mixed-format-properties-xml.txt");

        // After sub-segmentation: 2 blocks
        assertEquals(2, r.blocks().size(), "Should split into PROPERTIES and XML sub-blocks");

        // Verify block formats
        assertBlockFormat(r, Format.PROPERTIES);
        assertBlockFormat(r, Format.XML);

        // Verify flattened keys from both blocks
        assertTrue(r.flattened().containsKey("name"));
        assertTrue(r.flattened().containsKey("version"));
        assertTrue(r.flattened().containsKey("config.host"));
        assertTrue(r.flattened().containsKey("config.port"));
    }

    /**
     * JSON block followed by DOTENV export lines adjacent (no blank line separator).
     * Should produce 2 sub-blocks: JSON and DOTENV.
     */
    @Test
    void mixedFormatJsonDotenv() {
        ParseResult r = parseResource("edge-cases/mixed-format-json-dotenv.txt");

        assertEquals(2, r.blocks().size(), "Should split into JSON and DOTENV sub-blocks");

        assertBlockFormat(r, Format.JSON);
        assertBlockFormat(r, Format.DOTENV);

        assertTrue(r.flattened().containsKey("name"));
        assertTrue(r.flattened().containsKey("version"));
        assertTrue(r.flattened().containsKey("DATABASE_HOST"));
        assertTrue(r.flattened().containsKey("DATABASE_PORT"));
    }

    /**
     * DOTENV export lines followed by XML adjacent (no blank line separator).
     * Should produce 2 sub-blocks: DOTENV and XML.
     */
    @Test
    void mixedFormatDotenvXml() {
        ParseResult r = parseResource("edge-cases/mixed-format-dotenv-xml.txt");

        assertEquals(2, r.blocks().size(), "Should split into DOTENV and XML sub-blocks");

        assertBlockFormat(r, Format.DOTENV);
        assertBlockFormat(r, Format.XML);

        assertTrue(r.flattened().containsKey("APP_NAME"));
        assertTrue(r.flattened().containsKey("APP_PORT"));
        assertTrue(r.flattened().containsKey("config.debug"));
    }

    /**
     * Multi-line JSON and XML constructs adjacent but not separated by blank lines.
     * Each multi-line construct should stay intact (no internal split inside braces).
     * Expect 2 sub-blocks: JSON and XML.
     */
    @Test
    void mixedFormatMultilineSafe() {
        ParseResult r = parseResource("edge-cases/mixed-format-multiline-safe.txt");
        assertNoErrors(r, "mixed-format-multiline-safe");

        // 2 blocks: JSON (with nested braces intact) and XML
        assertEquals(2, r.blocks().size(), "Should split into JSON and XML sub-blocks, not split internally");

        assertBlockFormat(r, Format.JSON);
        assertBlockFormat(r, Format.XML);

        assertTrue(r.flattened().containsKey("name"));
        assertTrue(r.flattened().containsKey("config.debug"));
        assertTrue(r.flattened().containsKey("config.port"));
        assertTrue(r.flattened().containsKey("server.host"));
    }

    /**
     * Hinted block with mixed formats. The hint should be honored;
     * no sub-segmentation should occur inside a hinted block.
     * Expect 1 hinted block with PROPERTIES format.
     */
    @Test
    void mixedFormatHinted() {
        ParseResult r = parseResource("edge-cases/mixed-format-hinted.txt");
        assertNoErrors(r, "mixed-format-hinted");

        assertEquals(1, r.blocks().size(), "Hinted block should not be sub-segmented");
        // The block should be marked as hinted with PROPERTIES format
        assertTrue(r.blocks().get(0).hinted(), "Block should be hinted");
        assertEquals(Format.PROPERTIES, r.blocks().get(0).detectedFormat());

        assertTrue(r.flattened().containsKey("key"));
    }

    private static void assertBlockFormat(ParseResult result, Format expectedFormat) {
        assertTrue(
                result.blocks().stream().anyMatch(b -> b.detectedFormat() == expectedFormat),
                "Expected a block with format " + expectedFormat.name()
                        + " but found: " + result.blocks().stream()
                        .map(b -> b.detectedFormat().name())
                        .toList()
        );
    }
}
