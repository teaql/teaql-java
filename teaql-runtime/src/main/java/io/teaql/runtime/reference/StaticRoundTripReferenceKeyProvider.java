package io.teaql.runtime.reference;

import java.util.LinkedHashMap;
import java.util.Map;

/** In-memory key ring for configuration adapters, tests, and examples. */
public final class StaticRoundTripReferenceKeyProvider implements RoundTripReferenceKeyProvider {
    private final RoundTripReferenceKey current;
    private final Map<String, RoundTripReferenceKey> keys;

    public StaticRoundTripReferenceKeyProvider(
            RoundTripReferenceKey current, RoundTripReferenceKey... decodeOnlyKeys) {
        if (current == null) throw new IllegalArgumentException("current key must not be null");
        this.current = current;
        Map<String, RoundTripReferenceKey> configured = new LinkedHashMap<>();
        configured.put(current.keyId(), current);
        if (decodeOnlyKeys != null) {
            for (RoundTripReferenceKey key : decodeOnlyKeys) {
                if (key == null || configured.put(key.keyId(), key) != null) {
                    throw new IllegalArgumentException("duplicate or null reference key");
                }
            }
        }
        this.keys = Map.copyOf(configured);
    }

    @Override
    public RoundTripReferenceKey currentKey() { return current; }

    @Override
    public RoundTripReferenceKey keyById(String keyId) { return keys.get(keyId); }
}
