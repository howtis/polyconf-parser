package com.polyconf.parser.model;

import com.polyconf.parser.classify.DotenvDetector;
import com.polyconf.parser.classify.FormatDetector;
import com.polyconf.parser.classify.HoconDetector;
import com.polyconf.parser.classify.IniDetector;
import com.polyconf.parser.classify.Json5Detector;
import com.polyconf.parser.classify.JsonDetector;
import com.polyconf.parser.classify.PropertiesDetector;
import com.polyconf.parser.classify.TomlDetector;
import com.polyconf.parser.classify.XmlDetector;
import com.polyconf.parser.classify.YamlDetector;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class Format {
    private static final ConcurrentHashMap<String, Format> REGISTRY = new ConcurrentHashMap<>();

    private final String name;
    private final FormatDetector detector;

    private Format(String name, FormatDetector detector) {
        this.name = name;
        this.detector = detector;
    }

    public String name() {
        return name;
    }

    public Optional<FormatDetector> detector() {
        return Optional.ofNullable(detector);
    }

    public static Format register(String name, FormatDetector detector) {
        Format format = new Format(name, detector);
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

    // Constants
    public static final Format UNKNOWN = new Format("UNKNOWN", null);
    public static final Format TOML = register("TOML", new TomlDetector());
    public static final Format YAML = register("YAML", new YamlDetector());
    public static final Format PROPERTIES = register("PROPERTIES", new PropertiesDetector());
    public static final Format INI = register("INI", new IniDetector());
    public static final Format JSON = register("JSON", new JsonDetector());
    public static final Format DOTENV = register("DOTENV", new DotenvDetector());
    public static final Format XML = register("XML", new XmlDetector());
    public static final Format HOCON = register("HOCON", new HoconDetector());
    public static final Format JSON5 = register("JSON5", new Json5Detector());

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
