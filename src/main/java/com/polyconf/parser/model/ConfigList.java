package com.polyconf.parser.model;

import java.util.List;

public record ConfigList(
        String key,
        List<ConfigNode> items,
        Provenance provenance,
        String path
) implements ConfigNode {

    public ConfigList {
        if (items == null) {
            throw new IllegalArgumentException("items must not be null");
        }
    }
}
