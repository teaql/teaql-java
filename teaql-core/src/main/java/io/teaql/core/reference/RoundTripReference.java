package io.teaql.core.reference;

/** Opaque wire value issued by a TeaQL runtime and returned to that runtime. */
public record RoundTripReference(String value) {
    public RoundTripReference {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
