package com.polyconf.parser.model;

import com.polyconf.parser.classify.FormatDetector;
import com.polyconf.parser.format.DotenvFormat;
import com.polyconf.parser.format.HoconFormat;
import com.polyconf.parser.format.IniFormat;
import com.polyconf.parser.format.Json5Format;
import com.polyconf.parser.format.JsonFormat;
import com.polyconf.parser.format.KdlFormat;
import com.polyconf.parser.format.PropertiesFormat;
import com.polyconf.parser.format.TomlFormat;
import com.polyconf.parser.format.XmlFormat;
import com.polyconf.parser.format.YamlFormat;
import com.polyconf.parser.parse.LenientParser;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class Format {
    private static final ConcurrentHashMap<String, Format> REGISTRY = new ConcurrentHashMap<>();

    private final String name;
    private final FormatDetector detector;
    private final LenientParser parser;
    private final int trialPriority;

    private Format(String name, FormatDetector detector, LenientParser parser, int trialPriority) {
        this.name = name;
        this.detector = detector;
        this.parser = parser;
        this.trialPriority = trialPriority;
    }

    public String name() {
        return name;
    }

    public Optional<FormatDetector> detector() {
        return Optional.ofNullable(detector);
    }

    public Optional<LenientParser> parser() {
        return Optional.ofNullable(parser);
    }

    /**
     * Priority for trial-and-error parsing. Higher values are tried first.
     * Default 0 means this format does not participate in trial-and-error.
     */
    public int trialPriority() {
        return trialPriority;
    }

    public static Format register(String name, FormatDetector detector, LenientParser parser, int trialPriority) {
        Format format = new Format(name, detector, parser, trialPriority);
        REGISTRY.put(name, format);
        return format;
    }

    public static Format valueOf(String name) {
        Format format = REGISTRY.get(name);
        if (format == null) {
            throw new IllegalArgumentException("No Format registered with name: " + name);
        }
        return format;
    }

    public static List<Format> registeredFormats() {
        return List.copyOf(REGISTRY.values());
    }

    // Constants -- trial-and-error order: specific parsers first, superset parsers later, lenient last.
    // INI only activates on [section] markers to avoid matching key=value (Properties format).
    public static final Format UNKNOWN = new Format("UNKNOWN", null, null, 0);
    public static final Format XML = register("XML", new XmlFormat.Detector(), new XmlFormat.Parser(), 100);
    public static final Format JSON = register("JSON", new JsonFormat.Detector(), new JsonFormat.Parser(), 90);
    public static final Format JSON5 = register("JSON5", new Json5Format.Detector(), new Json5Format.Parser(), 80);
    public static final Format TOML = register("TOML", new TomlFormat.Detector(), new TomlFormat.Parser(), 70);
    public static final Format KDL = register("KDL", new KdlFormat.Detector(), new KdlFormat.Parser(), 60);
    public static final Format YAML = register("YAML", new YamlFormat.Detector(), new YamlFormat.Parser(), 50);
    public static final Format INI = register("INI", new IniFormat.Detector(), new IniFormat.Parser(), 40);
    public static final Format PROPERTIES = register("PROPERTIES", new PropertiesFormat.Detector(), new PropertiesFormat.Parser(), 30);
    public static final Format HOCON = register("HOCON", new HoconFormat.Detector(), new HoconFormat.Parser(), 20);
    public static final Format DOTENV = register("DOTENV", new DotenvFormat.Detector(), new DotenvFormat.Parser(), 10);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Format format)) return false;
        return Objects.equals(name, format.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
