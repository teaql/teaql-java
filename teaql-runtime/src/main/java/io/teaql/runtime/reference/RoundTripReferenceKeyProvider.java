package io.teaql.runtime.reference;

/** Supplies one current encryption key and retained decode-only rotation keys. */
public interface RoundTripReferenceKeyProvider {
    RoundTripReferenceKey currentKey();

    RoundTripReferenceKey keyById(String keyId);
}
