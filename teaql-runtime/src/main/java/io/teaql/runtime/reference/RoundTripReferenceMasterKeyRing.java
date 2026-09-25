package io.teaql.runtime.reference;

public interface RoundTripReferenceMasterKeyRing {
    RoundTripReferenceKey currentKey();

    RoundTripReferenceKey keyById(String keyId);
}
