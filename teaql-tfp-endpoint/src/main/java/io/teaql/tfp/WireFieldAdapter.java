package io.teaql.tfp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.Map;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.checker.WireLocationSegment;

/** Strict boundary adapter shared by TFP mutation and generated HTTP input adapters. */
public final class WireFieldAdapter {
    private WireFieldAdapter() {}

    public static NormalizedWireObject normalize(ObjectNode submitted, WireEntityMetadata metadata) {
        ObjectNode canonical = JsonNodeFactory.instance.objectNode();
        Map<String, String> paths = new LinkedHashMap<>();
        submitted.fields().forEachRemaining(entry -> {
            String canonicalName = metadata.canonicalField(entry.getKey());
            if (canonicalName == null) {
                throw new TfpEndpointException("WIRE_UNKNOWN_FIELD",
                        "Unknown field at /" + escape(entry.getKey()));
            }
            if (canonical.has(canonicalName)) {
                throw new TfpEndpointException("WIRE_FIELD_COLLISION",
                        "Multiple submitted fields resolve to " + canonicalName);
            }
            canonical.set(canonicalName, entry.getValue());
            paths.put(canonicalName, "/" + escape(entry.getKey()));
        });
        return new NormalizedWireObject(canonical, Map.copyOf(paths));
    }

    public static ObjectNode encode(ObjectNode canonical, WireEntityMetadata metadata) {
        ObjectNode wire = JsonNodeFactory.instance.objectNode();
        canonical.fields().forEachRemaining(entry -> {
            String wireName = metadata.wireField(entry.getKey());
            if (wireName == null) {
                throw new TfpEndpointException("WIRE_UNKNOWN_FIELD",
                        "Unknown canonical field: " + entry.getKey());
            }
            wire.set(wireName, entry.getValue());
        });
        return wire;
    }

    /** Adds the submitted alias path without changing the canonical checker location. */
    public static void retainSubmittedPaths(Iterable<CheckResult> results,
            NormalizedWireObject normalized) {
        for (CheckResult result : results) {
            if (result.getLocation() == null || result.getLocation().segments().isEmpty()) continue;
            WireLocationSegment first = result.getLocation().segments().get(0);
            if (!"property".equals(first.kind())) continue;
            String path = normalized.sourceInstancePaths().get(first.name());
            if (path != null) result.setSourceInstancePath(path);
        }
    }

    private static String escape(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }
}
