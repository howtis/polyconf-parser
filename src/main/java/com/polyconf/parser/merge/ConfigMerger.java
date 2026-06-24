package com.polyconf.parser.merge;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.DiagnosticLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigMerger {

    public MergeResult merge(List<ConfigSection> sections) {
        return merge(sections, MergePolicy.DEFAULT);
    }

    public MergeResult merge(List<ConfigSection> sections, MergePolicy policy) {
        if (sections == null) {
            throw new IllegalArgumentException("sections must not be null");
        }
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }

        List<BlockDiagnostic> diagnostics = new ArrayList<>();
        ConfigSection merged = new ConfigSection("", null, "");

        for (int i = 0; i < sections.size(); i++) {
            ConfigSection section = sections.get(i);
            if (section == null) {
                continue;
            }
            merged = mergeSection(merged, section, i, diagnostics, policy);
        }

        return new MergeResult(merged, List.copyOf(diagnostics));
    }

    private ConfigSection mergeSection(
            ConfigSection target,
            ConfigSection source,
            int blockIndex,
            List<BlockDiagnostic> diagnostics,
            MergePolicy policy
    ) {
        Map<String, ConfigNode> mergedChildren = new LinkedHashMap<>(target.children());

        for (var entry : source.children().entrySet()) {
            String key = entry.getKey();
            ConfigNode sourceNode = entry.getValue();

            if (!mergedChildren.containsKey(key)) {
                mergedChildren.put(key, sourceNode);
            } else {
                ConfigNode existing = mergedChildren.get(key);
                ConfigNode resolved = resolveCollision(
                        existing, sourceNode, key, blockIndex, diagnostics, policy);
                mergedChildren.put(key, resolved);
            }
        }

        return new ConfigSection(target.key(), mergedChildren, source.provenance(), target.path());
    }

    private ConfigNode resolveCollision(
            ConfigNode existing,
            ConfigNode incoming,
            String key,
            int blockIndex,
            List<BlockDiagnostic> diagnostics,
            MergePolicy policy
    ) {
        if (existing instanceof ConfigSection existingSection
                && incoming instanceof ConfigSection incomingSection) {
            return mergeSection(existingSection, incomingSection, blockIndex, diagnostics, policy);
        }

        boolean sameType = existing.getClass() == incoming.getClass();

        if (!sameType) {
            return handleTypeConflict(existing, incoming, key, blockIndex, diagnostics, policy);
        }

        return handleValueCollision(existing, incoming, key, blockIndex, diagnostics, policy);
    }

    private ConfigNode handleValueCollision(
            ConfigNode existing,
            ConfigNode incoming,
            String key,
            int blockIndex,
            List<BlockDiagnostic> diagnostics,
            MergePolicy policy
    ) {
        switch (policy.collisionMode()) {
            case REJECT:
                throw new MergeConflictException(
                        "Collision detected for key '" + key
                        + "' in block " + blockIndex);
            case WARN:
                diagnostics.add(new BlockDiagnostic(
                        blockIndex, blockIndex,
                        "Collision for key '" + key + "' -- overwriting",
                        DiagnosticLevel.WARNING));
                return incoming;
            case OVERWRITE:
            default:
                return incoming;
        }
    }

    private ConfigNode handleTypeConflict(
            ConfigNode existing,
            ConfigNode incoming,
            String key,
            int blockIndex,
            List<BlockDiagnostic> diagnostics,
            MergePolicy policy
    ) {
        String typeMsg = "Type conflict for key '" + key + "' in block " + blockIndex
                + ": " + nodeTypeName(existing) + " vs " + nodeTypeName(incoming);
        switch (policy.typeConflictMode()) {
            case REJECT:
                throw new MergeConflictException(typeMsg);
            case WARN:
                diagnostics.add(new BlockDiagnostic(
                        blockIndex, blockIndex,
                        typeMsg + " -- overwriting with " + nodeTypeName(incoming),
                        DiagnosticLevel.WARNING));
                return incoming;
            case OVERWRITE:
            default:
                return incoming;
        }
    }

    private static String nodeTypeName(ConfigNode node) {
        if (node instanceof ConfigSection) {
            return "section";
        }
        if (node instanceof ConfigList) {
            return "list";
        }
        if (node instanceof ConfigValue) {
            return "value";
        }
        return node.getClass().getSimpleName();
    }
}
