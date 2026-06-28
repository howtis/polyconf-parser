package com.polyconf.parser;

import com.polyconf.parser.classify.FormatClassifier;
import com.polyconf.parser.hint.Hint;
import com.polyconf.parser.hint.HintParser;
import com.polyconf.parser.merge.ConfigMerger;
import com.polyconf.parser.merge.MergePolicy;
import com.polyconf.parser.merge.MergeResult;
import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.BlockResult;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.model.Provenance;
import com.polyconf.parser.format.PropertiesFormat;
import com.polyconf.parser.parse.LenientParser;
import com.polyconf.parser.parse.ValueInference;
import com.polyconf.parser.segment.Segment;
import com.polyconf.parser.segment.Segmenter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PolyconfParser {

    // Trial-and-error order derived from Format.trialPriority().
    // Formats with trialPriority > 0 participate, sorted descending.
    private static final List<Format> TRIAL_FORMATS = Format.registeredFormats().stream()
            .filter(f -> f.trialPriority() > 0)
            .sorted(Comparator.comparingInt(Format::trialPriority).reversed())
            .toList();

    /**
     * Fallback parser used when no format is detected or the detected format's
     * parser produces an empty section. PropertiesParser is the most lenient
     * key=value extractor - it finds any '=' or ':' separator regardless of
     * surrounding whitespace.
     */
    private static final LenientParser FALLBACK_PARSER = new PropertiesFormat.Parser();

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
        segments = segments.stream()
                .flatMap(s -> subSegment(s, lines, hints).stream())
                .toList();

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

            if (currentFormat == nextFormat
                    || currentFormat == Format.UNKNOWN
                    || nextFormat == Format.UNKNOWN) {
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
            LenientParser parser = detectedFormat.parser().orElse(null);
            if (parser != null) {
                ParserResult pr = parser.parse(blockLines);
                if (!pr.section().children().isEmpty()) {
                    diagnostics.addAll(pr.diagnostics());
                    return new BlockResult(startLine, endLine, detectedFormat, 1.0, false, pr.section());
                }
                diagnostics.addAll(pr.diagnostics());
            }
        }

        // Classifier couldn't decide -- trial-and-error fallback.
        BlockResult trialResult = tryTrialAndError(blockLines, startLine, endLine, diagnostics);
        if (trialResult != null) {
            // If trial-and-error produced only flat key=value pairs (no sections),
            // try indentation-based recovery — it may preserve structure that
            // flat parsers like PropertiesParser ignore.
            if (isFlat(trialResult.section())) {
                ConfigSection recovered = recoverStructuredFallback(blockLines, startLine);
                if (hasStructure(recovered)) {
                    diagnostics.add(new BlockDiagnostic(startLine, endLine,
                            "Recovered structure via indentation-based fallback", DiagnosticLevel.WARNING));
                    return new BlockResult(startLine, endLine, Format.UNKNOWN, 0.15, false, recovered);
                }
            }
            return trialResult;
        }

        // Nothing matched — try indentation-based structure recovery
        ConfigSection recovered = recoverStructuredFallback(blockLines, startLine);
        if (!recovered.children().isEmpty()) {
            diagnostics.add(new BlockDiagnostic(startLine, endLine,
                    "Recovered structure via indentation-based fallback", DiagnosticLevel.WARNING));
            return new BlockResult(startLine, endLine, Format.UNKNOWN, 0.15, false, recovered);
        }

        // Last resort: flat PropertiesParser
        ParserResult fallbackResult = FALLBACK_PARSER.parse(blockLines);
        diagnostics.addAll(fallbackResult.diagnostics());
        return new BlockResult(startLine, endLine, Format.UNKNOWN, 0.0, false, fallbackResult.section());
    }

    private BlockResult tryTrialAndError(
            List<String> blockLines,
            int startLine,
            int endLine,
            List<BlockDiagnostic> diagnostics
    ) {
        for (Format format : TRIAL_FORMATS) {
            LenientParser parser = format.parser().orElseThrow();
            if (!parser.isPlausible(blockLines)) {
                continue;
            }
            ParserResult pr = parser.parse(blockLines);
            if (!pr.section().children().isEmpty()) {
                diagnostics.addAll(pr.diagnostics());
                return new BlockResult(startLine, endLine, format, 0.5, false, pr.section());
            }
        }
        return null;
    }

    private static boolean isFlat(ConfigSection section) {
        if (section.children().isEmpty()) {
            return false; // empty is not "flat"
        }
        return section.children().values().stream().noneMatch(n -> n instanceof ConfigSection);
    }

    private static boolean hasStructure(ConfigSection section) {
        return section.children().values().stream().anyMatch(n -> n instanceof ConfigSection);
    }

    /**
     * Attempts to recover indentation-based structure from key-value lines.
     * Used as a structured fallback when no format parser succeeds.
     *
     * <p>The algorithm:
     * <ol>
     *   <li>Compute leading whitespace depth for each line.</li>
     *   <li>For each non-blank, non-comment line, find the first '=' or ':' separator.</li>
     *   <li>Lines with no value after the separator become section headers.</li>
     *   <li>Lines with a value become key-value pairs, placed under the current
     *       section (determined by indentation depth).</li>
     *   <li>Dotted keys (e.g., {@code database.host}) create nested sections.</li>
     * </ol>
     */
    private static ConfigSection recoverStructuredFallback(List<String> blockLines, int startLine) {
        int n = blockLines.size();
        int[] indents = new int[n];
        for (int i = 0; i < n; i++) {
            indents[i] = computeIndent(blockLines.get(i));
        }

        Map<String, ConfigNode> rootChildren = new LinkedHashMap<>();
        Deque<Context> stack = new ArrayDeque<>();
        stack.push(new Context(-1, rootChildren));

        for (int i = 0; i < n; i++) {
            String raw = blockLines.get(i);
            String trimmed = raw.strip();

            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }

            int eqIdx = trimmed.indexOf('=');
            int colIdx = trimmed.indexOf(':');
            int sepIdx = (eqIdx >= 0 && colIdx >= 0) ? Math.min(eqIdx, colIdx) : Math.max(eqIdx, colIdx);
            if (sepIdx < 0) {
                continue;
            }

            String key = trimmed.substring(0, sepIdx).strip();
            String value = trimmed.substring(sepIdx + 1).strip();
            int indent = indents[i];

            // Pop stack until parent indentation is less than current
            while (stack.size() > 1 && stack.peek().indent >= indent) {
                stack.pop();
            }
            Map<String, ConfigNode> parent = stack.peek().children;

            if (value.isEmpty()) {
                // Section header
                ConfigSection section = new ConfigSection(key,
                        new LinkedHashMap<>(),
                        new Provenance(startLine + i, null, raw, 0.15),
                        "");
                ConfigNode existing = parent.get(key);
                if (existing instanceof ConfigSection s) {
                    for (var entry : s.children().entrySet()) {
                        section.children().putIfAbsent(entry.getKey(), entry.getValue());
                    }
                } else if (existing instanceof ConfigValue v) {
                    section.children().put("#self", v);
                }
                parent.put(key, section);
                stack.push(new Context(indent, section.children()));
            } else {
                // Key-value pair
                insertDottedKey(parent, key, value,
                        new Provenance(startLine + i, null, raw, 0.15));
            }
        }

        return new ConfigSection("", rootChildren, null, "");
    }

    private static void insertDottedKey(
            Map<String, ConfigNode> parent,
            String key,
            String value,
            Provenance provenance
    ) {
        String[] parts = key.split("\\.");
        if (parts.length > 1) {
            Map<String, ConfigNode> current = parent;
            for (int p = 0; p < parts.length - 1; p++) {
                String part = parts[p];
                ConfigNode existing = current.get(part);
                if (existing instanceof ConfigSection s) {
                    current = s.children();
                } else {
                    ConfigSection ns = new ConfigSection(part,
                            new LinkedHashMap<>(), null, "");
                    if (existing instanceof ConfigValue v) {
                        ns.children().put("#self", v);
                    }
                    current.put(part, ns);
                    current = ns.children();
                }
            }
            String leaf = parts[parts.length - 1];
            current.put(leaf, ValueInference.createValue(leaf, value, provenance));
        } else {
            parent.put(key, ValueInference.createValue(key, value, provenance));
        }
    }

    private static int computeIndent(String line) {
        int indent = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == ' ' || ch == '\t') {
                indent++;
            } else {
                break;
            }
        }
        return indent;
    }

    private static final class Context {
        final int indent;
        final Map<String, ConfigNode> children;
        Context(int indent, Map<String, ConfigNode> children) {
            this.indent = indent;
            this.children = children;
        }
    }

    private List<Segment> subSegment(Segment block, List<String> lines, List<Hint> hints) {
        // Skip hinted blocks — user intent takes priority
        if (findHint(hints, block.startLine()) != null) {
            return List.of(block);
        }
        // Single-line blocks have no room for internal splits
        if (block.startLine() == block.endLine()) {
            return List.of(block);
        }

        List<String> blockLines = lines.subList(block.startLine(), block.endLine() + 1);
        List<Format> perLineFormats = FormatClassifier.classifyPerLine(blockLines);
        List<Integer> depths = Segmenter.computeLineDepths(blockLines);

        int n = perLineFormats.size();

        // Walk through lines and find split boundaries where:
        // 1. Two adjacent lines have different non-UNKNOWN formats
        // 2. The left line ends at depth 0 (complete at the top level)
        // 3. The format change has strong structural evidence
        List<Integer> splitAfter = new ArrayList<>();
        for (int i = 0; i < n - 1; i++) {
            Format fmtA = perLineFormats.get(i);
            Format fmtB = perLineFormats.get(i + 1);
            if (fmtA == Format.UNKNOWN || fmtB == Format.UNKNOWN || fmtA == fmtB) {
                continue;
            }
            if (depths.get(i) != 0) {
                continue;
            }
            if (!isStrongBoundary(fmtA, blockLines.get(i), fmtB, blockLines.get(i + 1))) {
                continue;
            }
            splitAfter.add(i);
        }

        if (splitAfter.isEmpty()) {
            return List.of(block);
        }

        List<Segment> result = new ArrayList<>();
        int start = 0;
        for (int splitIdx : splitAfter) {
            result.add(new Segment(block.startLine() + start, block.startLine() + splitIdx));
            start = splitIdx + 1;
        }
        result.add(new Segment(block.startLine() + start, block.endLine()));
        return result;
    }

    /**
     * Returns true when the format boundary has strong structural evidence.
     * Accepts the boundary if:
     * - Either format is XML (unique {@code <} delimiter), OR
     * - Either anchor line is solely a JSON brace/bracket ({ } [ ]),
     *   indicating a clear structural delimiter, not KDL/HOCON syntax.
     * This prevents spurious splits between key=value-based formats
     * (TOML, PROPERTIES, INI, KDL) that share syntax.
     */
    private static boolean isStrongBoundary(Format fmtA, String lineA, Format fmtB, String lineB) {
        if (fmtA == Format.XML || fmtB == Format.XML) {
            return true;
        }
        return isBareJsonDelimiter(lineA) || isBareJsonDelimiter(lineB);
    }

    private static boolean isBareJsonDelimiter(String line) {
        String t = line.strip();
        return t.equals("{") || t.equals("}");
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
        return format.parser().orElseThrow(() ->
                new IllegalArgumentException("No parser registered for format: " + format.name()));
    }
}
