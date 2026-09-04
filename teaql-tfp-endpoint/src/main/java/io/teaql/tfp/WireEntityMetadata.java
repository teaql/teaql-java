package io.teaql.tfp;

import java.util.LinkedHashMap;
import java.util.Map;

/** Generated, serializer-independent mapping between wire names and canonical KSML fields. */
public final class WireEntityMetadata {
    private final String entity;
    private final Map<String, String> acceptedToCanonical;
    private final Map<String, String> canonicalToWire;

    public WireEntityMetadata(String entity, Map<String, String> canonicalToWire,
            Map<String, String> aliases) {
        this.entity = entity;
        this.canonicalToWire = Map.copyOf(canonicalToWire);
        Map<String, String> accepted = new LinkedHashMap<>();
        canonicalToWire.forEach((canonical, wire) -> {
            register(accepted, canonical, canonical);
            register(accepted, wire, canonical);
        });
        aliases.forEach((alias, canonical) -> {
            if (!canonicalToWire.containsKey(canonical)) {
                throw new IllegalArgumentException("Unknown canonical field for alias: " + canonical);
            }
            register(accepted, alias, canonical);
        });
        this.acceptedToCanonical = Map.copyOf(accepted);
    }

    /** Converts dependency-free metadata emitted by GeneratedRuntimeModule. */
    public static Map<String, WireEntityMetadata> fromGenerated(
            Map<String, Map<String, String>> mappings,
            Map<String, Map<String, String>> aliases) {
        Map<String, WireEntityMetadata> result = new LinkedHashMap<>();
        mappings.forEach((entity, fields) -> result.put(entity,
                new WireEntityMetadata(entity, fields, aliases.getOrDefault(entity, Map.of()))));
        return Map.copyOf(result);
    }

    private static void register(Map<String, String> accepted, String name, String canonical) {
        String previous = accepted.putIfAbsent(name, canonical);
        if (previous != null && !previous.equals(canonical)) {
            throw new IllegalArgumentException("Wire field alias is ambiguous: " + name);
        }
    }

    public String entity() { return entity; }

    public String canonicalField(String submitted) {
        return acceptedToCanonical.get(submitted);
    }

    public String wireField(String canonical) {
        return canonicalToWire.get(canonical);
    }

    Map<String, String> acceptedPolicyMap(Map<String, String> canonicalPolicy) {
        Map<String, String> result = new LinkedHashMap<>();
        acceptedToCanonical.forEach((accepted, canonical) -> {
            String internal = canonicalPolicy.get(canonical);
            if (internal != null) result.put(accepted, internal);
        });
        return Map.copyOf(result);
    }
}
