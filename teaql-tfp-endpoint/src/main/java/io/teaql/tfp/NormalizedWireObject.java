package io.teaql.tfp;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

/** Canonical payload plus the exact accepted JSON pointer used for each KSML field. */
public record NormalizedWireObject(ObjectNode values, Map<String, String> sourceInstancePaths) {}
