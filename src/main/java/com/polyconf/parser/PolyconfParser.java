package com.polyconf.parser;

import com.polyconf.parser.classify.FormatClassifier;
import com.polyconf.parser.hint.Hint;
import com.polyconf.parser.hint.HintParser;
import com.polyconf.parser.merge.ConfigMerger;
import com.polyconf.parser.merge.MergePolicy;
import com.polyconf.parser.merge.MergeResult;
import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.BlockResult;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.parse.DotenvParser;
import com.polyconf.parser.parse.HoconParser;
import com.polyconf.parser.parse.IniParser;
import com.polyconf.parser.parse.Json5Parser;
import com.polyconf.parser.parse.JsonParser;
import com.polyconf.parser.parse.KdlParser;
import com.polyconf.parser.parse.LenientParser;
import com.polyconf.parser.parse.PropertiesParser;
import com.polyconf.parser.parse.TomlParser;
import com.polyconf.parser.parse.XmlParser;
import com.polyconf.parser.parse.YamlParser;
import com.polyconf.parser.segment.Segment;
import com.polyconf.parser.segment.Segmenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PolyconfParser {

    // trial-and-error order -- specific parsers first, superset parsers later, lenient last.
    // INI only activates on [section] markers to avoid matching key=value (Properties format).
    // Use List to guarantee iteration order for trial-and-error.
    private static final List<Map.Entry<Format, LenientParser>> PARSERS = List.of(
            Map.entry(Format.XML, new XmlParser()),
            Map.entry(Format.JSON, new JsonParser()),
            Map.entry(Format.JSON5, new Json5Parser()),
            Map.entry(Format.TOML, new TomlParser()),
            Map.entry(Format.KDL, new KdlParser()),
            Map.entry(Format.YAML, new YamlParser()),
            Map.entry(Format.INI, new IniParser()),
            Map.entry(Format.PROPERTIES, new PropertiesParser()),
            Map.entry(Format.HOCON, new HoconParser()),
            Map.entry(Format.DOTENV, new DotenvParser())
    );

    private static final Map<Format, LenientParser> PARSER_MAP = PARSERS.stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Guards to prevent lenient parsers from matching non-intended formats.
    // Without these, INI matches key=value, KDL matches key=value as empty nodes, etc.
    private static boolean isPlausible(List<String> lines, Format format) {
        if (format == Format.INI) {
            return lines.stream().anyMatch(l -> l.strip().startsWith("["));
        }
        if (format == Format.KDL) {
            // KDL has {children}, semicolons, or node arguments/properties after the name
            String text = String.join(" ", lines);
            return text.contains("{") || text.contains(";");
        }
        return true;
    }

    /**
     * Fallback parser used when no format is detected or the detected format's
     * parser produces an empty section. PropertiesParser is the most lenient
     * key=value extractor - it finds any '=' or ':' separator regardless of
     * surrounding whitespace.
     */
    private static final LenientParser FALLBACK_PARSER = new PropertiesParser();

    private final ConfigMerger merger;

    public PolyconfParser() {
        this.merger = new ConfigMerger();
    }

    public ParseResult parse(String content) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        return parse(Arrays.asList(content.split("\\R")));
    }

    public ParseResult parse(String content, MergePolicy policy) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        return parse(Arrays.asList(content.split("\\R")), policy);
    }

    public ParseResult parse(List<String> lines) {
        return parse(lines, MergePolicy.DEFAULT);
    }

    public ParseResult parse(List<String> lines, MergePolicy policy) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }

        List<Hint> hints = HintParser.parse(lines);
        List<Segment> segments = Segmenter.segment(lines, hints);
        segments = mergeAdjacentSegments(segments, lines, hints);

        List<BlockResult> blockResults = new ArrayList<>();
        List<ConfigSection> sections = new ArrayList<>();
        List<BlockDiagnostic> allDiagnostics = new ArrayList<>();

        for (Segment segment : segments) {
            int startLine = segment.startLine();
            int endLine = segment.endLine();
            List<String> blockLines = lines.subList(startLine, endLine + 1);

            Hint hint = findHint(hints, startLine);
            BlockResult blockResult = hint != null
                    ? processHintedBlock(blockLines, hint, startLine, endLine, allDiagnostics)
                    : processClassifiedBlock(blockLines, startLine, endLine, allDiagnostics);

            blockResults.add(blockResult);
            sections.add(blockResult.section());
        }

        MergeResult mergeResult = merger.merge(sections, policy);

        List<BlockDiagnostic> diagnostics = new ArrayList<>();
        diagnostics.addAll(mergeResult.diagnostics());
        diagnostics.addAll(allDiagnostics);

        return new ParseResult(mergeResult.merged(), blockResults, diagnostics);
    }

    private List<Segment> mergeAdjacentSegments(
            List<Segment> segments, List<String> lines, List<Hint> hints) {
        if (segments.size() <= 1) {
            return segments;
        }

        java.util.Set<Integer> hintLines = hints.stream()
                .map(Hint::line)
                .collect(Collectors.toSet());

        List<Segment> merged = new ArrayList<>();
        Segment current = segments.get(0);

        for (int i = 1; i < segments.size(); i++) {
            Segment next = segments.get(i);

            boolean currentIsHinted = hintLines.contains(current.startLine());
            boolean nextIsHinted = hintLines.contains(next.startLine());

            if (currentIsHinted || nextIsHinted) {
                merged.add(current);
                current = next;
                continue;
            }

            List<String> currentLines = lines.subList(current.startLine(), current.endLine() + 1);
            List<String> nextLines = lines.subList(next.startLine(), next.endLine() + 1);
            Format currentFormat = FormatClassifier.classify(currentLines);
            Format nextFormat = FormatClassifier.classify(nextLines);

            if (currentFormat != Format.UNKNOWN && currentFormat.equals(nextFormat)) {
                current = new Segment(current.startLine(), next.endLine());
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return List.copyOf(merged);
    }

    private BlockResult processHintedBlock(
            List<String> blockLines,
            Hint hint,
            int startLine,
            int endLine,
            List<BlockDiagnostic> diagnostics
    ) {
        LenientParser parser = parserFor(hint.format());
        ParserResult pr = parser.parse(blockLines);
        diagnostics.addAll(pr.diagnostics());
        return new BlockResult(startLine, endLine, hint.format(), 1.0, true, pr.section());
    }

    private BlockResult processClassifiedBlock(
            List<String> blockLines,
            int startLine,
            int endLine,
            List<BlockDiagnostic> diagnostics
    ) {
        // Use FormatClassifier to detect the most likely format first.
        // Fall back to trial-and-error only when the classifier cannot decide.
        Format detectedFormat = FormatClassifier.classify(blockLines);
        if (detectedFormat != Format.UNKNOWN) {
            LenientParser parser = PARSER_MAP.get(detectedFormat);
            if (parser != null) {
                ParserResult pr = parser.parse(blockLines);
                diagnostics.addAll(pr.diagnostics());
                return new BlockResult(startLine, endLine, detectedFormat, 1.0, false, pr.section());
            }
        }

        // Classifier couldn't decide -- trial-and-error fallback.
        for (Map.Entry<Format, LenientParser> entry : PARSERS) {
            if (!isPlausible(blockLines, entry.getKey())) {
                continue;
            }
            ParserResult pr = entry.getValue().parse(blockLines);
            if (!pr.section().children().isEmpty()) {
                diagnostics.addAll(pr.diagnostics());
                return new BlockResult(startLine, endLine, entry.getKey(), 0.5, false, pr.section());
            }
        }

        // Nothing matched -- use fallback
        ParserResult fallbackResult = FALLBACK_PARSER.parse(blockLines);
        diagnostics.addAll(fallbackResult.diagnostics());
        return new BlockResult(startLine, endLine, Format.UNKNOWN, 0.0, false, fallbackResult.section());
    }

    private static Hint findHint(List<Hint> hints, int line) {
        for (Hint hint : hints) {
            if (hint.line() == line) {
                return hint;
            }
        }
        return null;
    }

    private static LenientParser parserFor(Format format) {
        LenientParser parser = PARSER_MAP.get(format);
        if (parser == null) {
            throw new IllegalArgumentException("No parser registered for format: " + format.name());
        }
        return parser;
    }
}
