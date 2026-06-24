package com.polyconf.parser.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record ConfigSection(
        String key,
        Map<String, ConfigNode> children,
        Provenance provenance,
        String path
) implements ConfigNode {

    public ConfigSection(String key, Provenance provenance, String path) {
        this(key, new LinkedHashMap<>(), provenance, path);
    }

    public ConfigSection {
        if (children == null) {
            throw new IllegalArgumentException("children must not be null");
        }
    }
}
