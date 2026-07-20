package io.teaql.data.dynamic;

public final class DF {
    private DF() {}

    public static DynamicFieldSelection fields() {
        return new DynamicFieldSelection();
    }
}
