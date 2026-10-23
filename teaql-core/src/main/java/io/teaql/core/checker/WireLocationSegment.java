package io.teaql.core.checker;

/** Canonical KSML property or array-index segment used by wire violations. */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record WireLocationSegment(String kind, String name, Integer index) {
    public static WireLocationSegment property(String name) {
        return new WireLocationSegment("property", name, null);
    }

    public static WireLocationSegment index(int index) {
        return new WireLocationSegment("index", null, index);
    }
}
