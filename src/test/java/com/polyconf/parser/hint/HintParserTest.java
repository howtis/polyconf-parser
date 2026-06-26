package com.polyconf.parser.hint;

import com.polyconf.parser.model.Format;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HintParserTest {

    @Test
    void parseTomlHint() {
        List<String> lines = List.of("# @fmt:toml");
        List<Hint> hints = HintParser.parse(lines);

        assertEquals(1, hints.size());
        Hint h = hints.get(0);
        assertEquals(0, h.line());
        assertEquals(Format.TOML, h.format());
    }

    @Test
    void parseMultipleFormats() {
        List<String> lines = List.of(
                "# @fmt:json",
                "some content",
                "# @fmt:yaml",
                "# @fmt:toml"
        );
        List<Hint> hints = HintParser.parse(lines);

        assertEquals(3, hints.size());
        assertEquals(Format.JSON, hints.get(0).format());
        assertEquals(0, hints.get(0).line());
        assertEquals(Format.YAML, hints.get(1).format());
        assertEquals(2, hints.get(1).line());
        assertEquals(Format.TOML, hints.get(2).format());
        assertEquals(3, hints.get(2).line());
    }

    @Test
    void noHintsReturnsEmpty() {
        List<String> lines = List.of("key=value", "[section]", "port=5432");
        List<Hint> hints = HintParser.parse(lines);

        assertTrue(hints.isEmpty());
    }

    @Test
    void nullLinesThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                HintParser.parse(null));
    }

    @Test
    void unknownFormatReturnsUnknown() {
        List<String> lines = List.of("# @fmt:unknownfmt");
        List<Hint> hints = HintParser.parse(lines);

        assertEquals(1, hints.size());
        assertEquals(Format.UNKNOWN, hints.get(0).format());
    }
}
