package com.polyconf.parser.merge;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigSection;

import java.util.List;

public record MergeResult(
        ConfigSection merged,
        List<BlockDiagnostic> diagnostics
) {
    public MergeResult {
        if (merged == null) {
            throw new IllegalArgumentException("merged must not be null");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("diagnostics must not be null");
        }
    }
}
