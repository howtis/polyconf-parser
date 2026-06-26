package com.polyconf.parser.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class ConfigAccessor {
    private final ConfigNode root;

    public ConfigAccessor(ConfigNode root) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        this.root = root;
    }

    public Map<String, Object> asFlattenedMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        flatten(root, "", result);
        return result;
    }

    private void flatten(ConfigNode node, String prefix, Map<String, Object> result) {
        if (node instanceof ConfigValue value) {
            String key = prefix.isEmpty() ? value.key() : prefix + "." + value.key();
            result.put(key, value.rawValue());
        } else if (node instanceof ConfigSection section) {
            String keyPrefix = prefix.isEmpty() ? section.key() : prefix + "." + section.key();
            boolean resolvedSelf = false;
            for (ConfigNode child : section.children().values()) {
                if (child instanceof ConfigValue cv && isSelfMarker(cv.key())) {
                    result.put(keyPrefix, cv.rawValue());
                    resolvedSelf = true;
                } else {
                    flatten(child, keyPrefix, result);
                }
            }
            // sections without self-marker are structural parents; their children
            // are already flattened with the section's prefix above
        } else if (node instanceof ConfigList list) {
            String key = prefix.isEmpty() ? list.key() : prefix + "." + list.key();
            for (int i = 0; i < list.items().size(); i++) {
                ConfigNode item = list.items().get(i);
                String idxPrefix = key + "[" + i + "]";
                if (item instanceof ConfigValue v) {
                    result.put(idxPrefix, v.rawValue());
                } else if (item instanceof ConfigSection section) {
                    // Directly flatten children without adding section's internal key
                    for (ConfigNode child : section.children().values()) {
                        flatten(child, idxPrefix, result);
                    }
                } else {
                    flatten(item, idxPrefix, result);
                }
            }
        }
    }

    public Optional<String> getString(String dottedPath) {
        return root.resolve(dottedPath)
                .filter(n -> n instanceof ConfigValue)
                .map(n -> ((ConfigValue) n).asString())
                .flatMap(o -> o);
    }

    public Optional<Integer> getInt(String dottedPath) {
        return root.resolve(dottedPath)
                .filter(n -> n instanceof ConfigValue)
                .map(n -> ((ConfigValue) n).asInt())
                .flatMap(o -> o);
    }

    public Optional<Boolean> getBool(String dottedPath) {
        return root.resolve(dottedPath)
                .filter(n -> n instanceof ConfigValue)
                .map(n -> ((ConfigValue) n).asBool())
                .flatMap(o -> o);
    }

    public Optional<Double> getFloat(String dottedPath) {
        return root.resolve(dottedPath)
                .filter(n -> n instanceof ConfigValue)
                .map(n -> ((ConfigValue) n).asFloat())
                .flatMap(o -> o);
    }

    public Optional<ConfigSection> getSection(String dottedPath) {
        return root.resolve(dottedPath)
                .filter(n -> n instanceof ConfigSection)
                .map(n -> (ConfigSection) n);
    }

    public Optional<ConfigNode> get(String dottedPath) {
        return root.resolve(dottedPath);
    }

    public Stream<ConfigNode> walk() {
        return walk(root);
    }

    private static boolean isSelfMarker(String key) {
        return "#text".equals(key) || "#self".equals(key);
    }

    private Stream<ConfigNode> walk(ConfigNode node) {
        Stream<ConfigNode> self = Stream.of(node);
        Stream<ConfigNode> children;
        if (node instanceof ConfigSection section) {
            children = section.children().values().stream()
                    .flatMap(this::walk);
        } else if (node instanceof ConfigList list) {
            children = list.items().stream()
                    .flatMap(this::walk);
        } else {
            children = Stream.empty();
        }
        return Stream.concat(self, children);
    }
}
