package com.polyconf.parser;

import com.polyconf.parser.classify.FormatClassifier;
import com.polyconf.parser.compare.ResultDivergence;
import com.polyconf.parser.hint.Hint;
import com.polyconf.parser.hint.HintParser;
import com.polyconf.parser.merge.ConfigMerger;
import com.polyconf.parser.merge.MergePolicy;
import com.polyconf.parser.merge.MergeResult;
import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.BlockResult;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.parse.CsvParser;
import com.polyconf.parser.parse.DotenvParser;
import com.polyconf.parser.parse.IniParser;
import com.polyconf.parser.parse.JsonParser;
import com.polyconf.parser.parse.LenientParser;
import com.polyconf.parser.parse.PropertiesParser;
import com.polyconf.parser.parse.TomlParser;
import com.polyconf.parser.parse.XmlParser;
import com.polyconf.parser.parse.YamlParser;
import com.polyconf.parser.segment.Segment;
import com.polyconf.parser.segment.Segmenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PolyconfParser {

    private static final Map<Format, LenientParser> PARSERS = Map.ofEntries(
            Map.entry(Format.TOML, new TomlParser()),
            Map.entry(Format.YAML, new YamlParser()),
            Map.entry(Format.PROPERTIES, new PropertiesParser()),
            Map.entry(Format.INI, new IniParser()),
            Map.entry(Format.JSON, new JsonParser()),
            Map.entry(Format.DOTENV, new DotenvParser()),
            Map.entry(Format.XML, new XmlParser()),
            Map.entry(Format.CSV, new CsvParser())
    );

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
        Map<Format, Integer> scores = FormatClassifier.scoreMap(blockLines);
        List<Map.Entry<Format, Integer>> ranked = scores.entrySet().stream()
                .filter(e -> e.getKey() != Format.UNKNOWN)
                .sorted(Map.Entry.<Format, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        if (ranked.isEmpty()) {
            ParserResult fallbackResult = FALLBACK_PARSER.parse(blockLines);
            diagnostics.addAll(fallbackResult.diagnostics());
            return new BlockResult(startLine, endLine, Format.UNKNOWN, 0.0, false, fallbackResult.section());
        }

        Format primary = ranked.get(0).getKey();
        int primaryScore = ranked.get(0).getValue();

        if (ranked.size() == 1 || ranked.get(0).getValue() > ranked.get(1).getValue()) {
            int secondScore = ranked.size() > 1 ? ranked.get(1).getValue() : 0;
            double confidence = computeConfidence(primaryScore, secondScore);
            LenientParser parser = parserFor(primary);
            ParserResult pr = parser.parse(blockLines);
            diagnostics.addAll(pr.diagnostics());
            ConfigSection section = pr.section();
            if (section.children().isEmpty()) {
                ParserResult fallbackResult = FALLBACK_PARSER.parse(blockLines);
                if (!fallbackResult.section().children().isEmpty()) {
                    diagnostics.addAll(fallbackResult.diagnostics());
                    return new BlockResult(startLine, endLine, primary, confidence, false, fallbackResult.section());
                }
            }
            return new BlockResult(startLine, endLine, primary, confidence, false, section);
        }

        return processAmbiguousBlock(blockLines, ranked, startLine, endLine, diagnostics);
    }

    private BlockResult processAmbiguousBlock(
            List<String> blockLines,
            List<Map.Entry<Format, Integer>> ranked,
            int startLine,
            int endLine,
            List<BlockDiagnostic> diagnostics
    ) {
        Format primary = ranked.get(0).getKey();
        Format secondary = ranked.get(1).getKey();
        int score = ranked.get(0).getValue();
        double confidence = computeConfidence(score, score);

        ParserResult pr1 = parserFor(primary).parse(blockLines);
        ParserResult pr2 = parserFor(secondary).parse(blockLines);
        ConfigSection section1 = pr1.section();
        ConfigSection section2 = pr2.section();
        diagnostics.addAll(pr1.diagnostics());
        diagnostics.addAll(pr2.diagnostics());

        ResultDivergence.Level divergence = ResultDivergence.compare(section1, section2);

        if (divergence != ResultDivergence.Level.IDENTICAL) {
            DiagnosticLevel level = divergence == ResultDivergence.Level.TYPE_ONLY
                    ? DiagnosticLevel.WARNING
                    : DiagnosticLevel.ERROR;
            diagnostics.add(new BlockDiagnostic(
                    startLine, endLine,
                    "Ambiguous format ("
                            + primary.name() + " vs " + secondary.name()
                            + "): divergence=" + divergence.name().toLowerCase(),
                    level));
        }

        return new BlockResult(startLine, endLine, primary, confidence, false, section1);
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
        LenientParser parser = PARSERS.get(format);
        if (parser == null) {
            throw new IllegalArgumentException("No parser registered for format: " + format.name());
        }
        return parser;
    }

    private static double computeConfidence(int topScore, int secondScore) {
        if (topScore <= 0) {
            return 0.0;
        }
        if (secondScore <= 0) {
            return 1.0;
        }
        double ratio = (double) topScore / (topScore + secondScore);
        return Math.round(ratio * 100.0) / 100.0;
    }
}
