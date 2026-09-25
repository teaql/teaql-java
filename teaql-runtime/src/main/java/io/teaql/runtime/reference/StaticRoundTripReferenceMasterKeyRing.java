package io.teaql.runtime.reference;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small in-memory key ring for configuration adapters, examples and tests. */
public final class StaticRoundTripReferenceMasterKeyRing
        implements RoundTripReferenceMasterKeyRing {
    private final RoundTripReferenceKey current;
    private final Map<String, RoundTripReferenceKey> keys;

    public StaticRoundTripReferenceMasterKeyRing(
            RoundTripReferenceKey current, RoundTripReferenceKey... decodeOnlyKeys) {
        if (current == null) throw new IllegalArgumentException("current key must not be null");
        this.current = current;
        Map<String, RoundTripReferenceKey> configured = new LinkedHashMap<>();
        configured.put(current.keyId(), current);
        if (decodeOnlyKeys != null) {
            for (RoundTripReferenceKey key : decodeOnlyKeys) {
                if (key != null) configured.put(key.keyId(), key);
            }
        }
        this.keys = Map.copyOf(configured);
    }

    @Override
    public RoundTripReferenceKey currentKey() {
        return current;
    }

    @Override
    public RoundTripReferenceKey keyById(String keyId) {
        return keys.get(keyId);
    }
}
