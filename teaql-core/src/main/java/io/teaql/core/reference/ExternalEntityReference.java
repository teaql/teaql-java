package io.teaql.core.reference;

/** Strongly typed internal representation; Web adapters use {@link ReferenceWireCodec}. */
public sealed interface ExternalEntityReference {
    record Governed(String token) implements ExternalEntityReference {
        public Governed {
            if (token == null || token.isBlank()) throw new IllegalArgumentException("token required");
        }
    }

    record Raw(long id, long version) implements ExternalEntityReference {
        public Raw {
            if (id <= 0 || version < 0) throw new IllegalArgumentException("invalid raw identity");
        }
    }
}
