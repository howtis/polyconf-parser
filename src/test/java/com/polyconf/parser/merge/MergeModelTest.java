package com.polyconf.parser.merge;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.Provenance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MergeModelTest {

    // --- MergePolicy ---

    @Test
    void mergePolicyValid() {
        MergePolicy policy = new MergePolicy(CollisionMode.OVERWRITE, TypeConflictMode.WARN);
        assertEquals(CollisionMode.OVERWRITE, policy.collisionMode());
        assertEquals(TypeConflictMode.WARN, policy.typeConflictMode());
    }

    @Test
    void mergePolicyDefault() {
        assertEquals(CollisionMode.OVERWRITE, MergePolicy.DEFAULT.collisionMode());
        assertEquals(TypeConflictMode.WARN, MergePolicy.DEFAULT.typeConflictMode());
    }

    @Test
    void mergePolicyNullCollisionModeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new MergePolicy(null, TypeConflictMode.WARN));
    }

    @Test
    void mergePolicyNullTypeConflictModeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new MergePolicy(CollisionMode.OVERWRITE, null));
    }

    // --- MergeResult ---

    @Test
    void mergeResultValid() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection section = new ConfigSection("", p, "");
        MergeResult result = new MergeResult(section, List.of());

        assertSame(section, result.merged());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void mergeResultWithDiagnostics() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection section = new ConfigSection("", p, "");
        BlockDiagnostic diag = new BlockDiagnostic(1, 2, "type conflict", DiagnosticLevel.WARNING);
        MergeResult result = new MergeResult(section, List.of(diag));

        assertEquals(1, result.diagnostics().size());
        assertEquals("type conflict", result.diagnostics().get(0).message());
    }

    @Test
    void mergeResultNullMergedThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new MergeResult(null, List.of()));
    }

    @Test
    void mergeResultNullDiagnosticsThrows() {
        Provenance p = new Provenance(0, Format.TOML, "", 1.0);
        ConfigSection section = new ConfigSection("", p, "");
        assertThrows(IllegalArgumentException.class, () ->
                new MergeResult(section, null));
    }
}
