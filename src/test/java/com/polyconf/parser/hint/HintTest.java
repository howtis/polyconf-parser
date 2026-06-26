package com.polyconf.parser.hint;

import com.polyconf.parser.model.Format;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HintTest {

    @Test
    void validHint() {
        Hint hint = new Hint(3, Format.JSON);
        assertEquals(3, hint.line());
        assertEquals(Format.JSON, hint.format());
    }

    @Test
    void negativeLineThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new Hint(-1, Format.TOML));
    }

    @Test
    void nullFormatThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new Hint(0, null));
    }
}
