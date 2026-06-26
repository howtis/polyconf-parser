package com.polyconf.parser.segment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SegmentTest {

    @Test
    void validSegment() {
        Segment s = new Segment(0, 5);
        assertEquals(0, s.startLine());
        assertEquals(5, s.endLine());
    }

    @Test
    void singleLineSegment() {
        Segment s = new Segment(3, 3);
        assertEquals(3, s.startLine());
        assertEquals(3, s.endLine());
    }

    @Test
    void negativeStartLineThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new Segment(-1, 5));
    }

    @Test
    void endLineBeforeStartLineThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new Segment(5, 3));
    }
}
