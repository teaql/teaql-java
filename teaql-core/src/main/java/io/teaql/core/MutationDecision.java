package io.teaql.core;

import java.util.List;

public record MutationDecision(
        Verdict verdict, String code, String message, List<String> fieldPaths) {
    public enum Verdict { ALLOW, DENY }

    public MutationDecision {
        fieldPaths = List.copyOf(fieldPaths == null ? List.of() : fieldPaths);
        if (verdict == Verdict.DENY && (code == null || code.isBlank())) {
            throw new IllegalArgumentException("A denied mutation decision requires a stable code");
        }
    }

    public static MutationDecision allow() {
        return new MutationDecision(Verdict.ALLOW, null, null, List.of());
    }

    public static MutationDecision deny(String code, String message, List<String> fieldPaths) {
        return new MutationDecision(Verdict.DENY, code, message, fieldPaths);
    }

    public boolean allowed() { return verdict == Verdict.ALLOW; }
}
