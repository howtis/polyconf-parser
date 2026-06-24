package com.polyconf.parser.merge;

public record MergePolicy(
        CollisionMode collisionMode,
        TypeConflictMode typeConflictMode
) {
    public static final MergePolicy DEFAULT = new MergePolicy(
            CollisionMode.OVERWRITE,
            TypeConflictMode.WARN
    );

    public MergePolicy {
        if (collisionMode == null) {
            throw new IllegalArgumentException("collisionMode must not be null");
        }
        if (typeConflictMode == null) {
            throw new IllegalArgumentException("typeConflictMode must not be null");
        }
    }
}
