package io.teaql.runtime;

public record MutationGovernanceEvent(
        MutationGovernanceSnapshot snapshot, String warningCode, boolean firstOccurrence) {}
