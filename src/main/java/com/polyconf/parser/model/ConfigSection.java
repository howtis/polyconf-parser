package com.polyconf.parser.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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

    public Optional<ConfigValue> childValue(String key) {
        ConfigNode node = children.get(key);
        return node instanceof ConfigValue v ? Optional.of(v) : Optional.empty();
    }

    public Optional<ConfigSection> childSection(String key) {
        ConfigNode node = children.get(key);
        return node instanceof ConfigSection s ? Optional.of(s) : Optional.empty();
    }

    public Optional<ConfigList> childList(String key) {
        ConfigNode node = children.get(key);
        return node instanceof ConfigList l ? Optional.of(l) : Optional.empty();
    }
}
