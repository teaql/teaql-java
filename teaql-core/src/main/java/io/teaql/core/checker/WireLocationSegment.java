package io.teaql.core.checker;

/** Canonical KSML property or array-index segment used by wire violations. */
public record WireLocationSegment(String kind, String name, Integer index) {
    public static WireLocationSegment property(String name) {
        return new WireLocationSegment("property", name, null);
    }

    public static WireLocationSegment index(int index) {
        return new WireLocationSegment("index", null, index);
    }
}
