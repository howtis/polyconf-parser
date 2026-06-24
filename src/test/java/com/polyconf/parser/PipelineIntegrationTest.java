package com.polyconf.parser;

import com.polyconf.parser.classify.FormatClassifier;
import com.polyconf.parser.hint.Hint;
import com.polyconf.parser.hint.HintParser;
import com.polyconf.parser.model.Format;
import com.polyconf.parser.segment.Segment;
import com.polyconf.parser.segment.Segmenter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PipelineIntegrationTest {

    @Test
    void mixedFormatWithHints() {
        List<String> lines = List.of(
                "# @fmt:toml",
                "[server]",
                "host = \"localhost\"",
                "port = 5432",
                "",
                "# @fmt:yaml",
                "database:",
                "  host: localhost",
                "  port: 5432"
        );

        List<Hint> hints = HintParser.parse(lines);
        List<Segment> segments = Segmenter.segment(lines, hints);

        assertEquals(2, segments.size());

        Segment tomlBlock = segments.get(0);
        assertEquals(0, tomlBlock.startLine());
        assertEquals(3, tomlBlock.endLine());
        Format tomlFormat = FormatClassifier.classify(
                lines.subList(tomlBlock.startLine(), tomlBlock.endLine() + 1));
        assertEquals(Format.TOML, tomlFormat);

        Segment yamlBlock = segments.get(1);
        assertEquals(5, yamlBlock.startLine());
        assertEquals(8, yamlBlock.endLine());
        Format yamlFormat = FormatClassifier.classify(
                lines.subList(yamlBlock.startLine(), yamlBlock.endLine() + 1));
        assertEquals(Format.YAML, yamlFormat);
    }

    @Test
    void mixedFormatWithoutHints() {
        List<String> lines = List.of(
                "{",
                "  \"name\": \"app\"",
                "}",
                "",
                "server.host=localhost",
                "server.port=5432"
        );

        List<Hint> hints = HintParser.parse(lines);
        List<Segment> segments = Segmenter.segment(lines, hints);

        assertEquals(2, segments.size());

        Segment jsonBlock = segments.get(0);
        Format jsonFormat = FormatClassifier.classify(
                lines.subList(jsonBlock.startLine(), jsonBlock.endLine() + 1));
        assertEquals(Format.JSON, jsonFormat);

        Segment propBlock = segments.get(1);
        Format propFormat = FormatClassifier.classify(
                lines.subList(propBlock.startLine(), propBlock.endLine() + 1));
        assertEquals(Format.PROPERTIES, propFormat);
    }

    @Test
    void singleBlockNoHints() {
        List<String> lines = List.of(
                "<config>",
                "  <key>value</key>",
                "</config>"
        );

        List<Hint> hints = HintParser.parse(lines);
        List<Segment> segments = Segmenter.segment(lines, hints);

        assertEquals(1, segments.size());
        assertEquals(Format.XML, FormatClassifier.classify(lines));
    }

    @Test
    void hintOverridesClassifier() {
        List<String> lines = List.of(
                "# @fmt:json",
                "key = \"value\""
        );

        List<Hint> hints = HintParser.parse(lines);
        assertEquals(1, hints.size());
        assertEquals(Format.JSON, hints.get(0).format());
    }
}
