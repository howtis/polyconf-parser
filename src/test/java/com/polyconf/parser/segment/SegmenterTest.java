package com.polyconf.parser.segment;

import com.polyconf.parser.hint.Hint;
import com.polyconf.parser.model.Format;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SegmenterTest {

    @Test
    void singleBlockNoBlanks() {
        List<String> lines = List.of(
                "key=value",
                "port=5432"
        );
        List<Segment> result = Segmenter.segment(lines, List.of());

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).startLine());
        assertEquals(1, result.get(0).endLine());
    }

    @Test
    void blankLineSplitsBlock() {
        List<String> lines = List.of(
                "a=1",
                "b=2",
                "",
                "x: y"
        );
        List<Segment> result = Segmenter.segment(lines, List.of());

        assertEquals(2, result.size());
        assertEquals(0, result.get(0).startLine());
        assertEquals(1, result.get(0).endLine());
        assertEquals(3, result.get(1).startLine());
        assertEquals(3, result.get(1).endLine());
    }

    @Test
    void multipleBlankLinesBetweenBlocks() {
        List<String> lines = List.of(
                "a=1",
                "",
                "",
                "b=2"
        );
        List<Segment> result = Segmenter.segment(lines, List.of());

        assertEquals(2, result.size());
        assertEquals(0, result.get(0).startLine());
        assertEquals(0, result.get(0).endLine());
        assertEquals(3, result.get(1).startLine());
        assertEquals(3, result.get(1).endLine());
    }

    @Test
    void hintAsBoundary() {
        List<String> lines = List.of(
                "# @fmt:toml",
                "key=value",
                "",
                "# @fmt:yaml",
                "key: value"
        );
        List<Hint> hints = List.of(
                new Hint(0, Format.TOML),
                new Hint(3, Format.YAML)
        );
        List<Segment> result = Segmenter.segment(lines, hints);

        assertEquals(2, result.size());
        assertEquals(0, result.get(0).startLine());
        assertEquals(1, result.get(0).endLine());
        assertEquals(3, result.get(1).startLine());
        assertEquals(4, result.get(1).endLine());
    }

    @Test
    void leadingBlankLinesSkipped() {
        List<String> lines = List.of(
                "",
                "",
                "key=value"
        );
        List<Segment> result = Segmenter.segment(lines, List.of());

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).startLine());
        assertEquals(2, result.get(0).endLine());
    }

    @Test
    void trailingBlankLinesIgnored() {
        List<String> lines = List.of(
                "key=value",
                "",
                ""
        );
        List<Segment> result = Segmenter.segment(lines, List.of());

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).startLine());
        assertEquals(0, result.get(0).endLine());
    }

    @Test
    void emptyInputNoBlocks() {
        List<Segment> result = Segmenter.segment(List.of(), List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void allBlankLinesNoBlocks() {
        List<String> lines = List.of("", "  ", "\t");
        List<Segment> result = Segmenter.segment(lines, List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void singleLineBlock() {
        List<String> lines = List.of("key=value");
        List<Segment> result = Segmenter.segment(lines, List.of());

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).startLine());
        assertEquals(0, result.get(0).endLine());
    }

    @Test
    void whitespaceOnlyLinesAreBlanks() {
        List<String> lines = List.of(
                "a=1",
                "   ",
                "b=2"
        );
        List<Segment> result = Segmenter.segment(lines, List.of());

        assertEquals(2, result.size());
    }

    @Test
    void hintWithoutPrecedingBlank() {
        List<String> lines = List.of(
                "a=1",
                "# @fmt:yaml",
                "b: 2"
        );
        List<Hint> hints = List.of(new Hint(1, Format.YAML));
        List<Segment> result = Segmenter.segment(lines, hints);

        assertEquals(2, result.size());
        assertEquals(0, result.get(0).startLine());
        assertEquals(0, result.get(0).endLine());
        assertEquals(1, result.get(1).startLine());
        assertEquals(2, result.get(1).endLine());
    }
}
