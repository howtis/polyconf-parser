package com.polyconf.parser.model;

import java.util.Optional;

public sealed interface ConfigNode
        permits ConfigValue, ConfigSection, ConfigList {

    Provenance provenance();
    String path();

    default Optional<ConfigNode> resolve(String dottedPath) {
        if (dottedPath == null || dottedPath.isEmpty()) {
            return Optional.of(this);
        }
        int dotIdx = dottedPath.indexOf('.');
        if (dotIdx == -1) {
            if (this instanceof ConfigSection section) {
                return Optional.ofNullable(section.children().get(dottedPath));
            }
            return Optional.empty();
        }
        String firstKey = dottedPath.substring(0, dotIdx);
        String rest = dottedPath.substring(dotIdx + 1);
        if (this instanceof ConfigSection section) {
            ConfigNode child = section.children().get(firstKey);
            if (child != null) {
                return child.resolve(rest);
            }
        }
        return Optional.empty();
    }
}
