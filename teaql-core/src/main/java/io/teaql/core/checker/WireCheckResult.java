package io.teaql.core.checker;

import java.util.List;

/** Stable external projection of a Checker violation. */
public record WireCheckResult(
        String ruleId,
        String entityType,
        List<WireLocationSegment> location,
        String instancePath,
        String sourceInstancePath,
        Object inputValue,
        Object systemValue,
        String message) {}
