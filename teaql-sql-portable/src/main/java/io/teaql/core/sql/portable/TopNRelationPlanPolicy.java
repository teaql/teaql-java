package io.teaql.core.sql.portable;

/** Provider default for loading a bounded child collection per already-loaded parent. */
public enum TopNRelationPlanPolicy {
    WINDOW,
    ALWAYS_PROBE
}
