package io.teaql.runtime.reference;

import java.util.Map;

public record ReferenceStartupNotice(
        String level,
        String message,
        String deploymentProfile,
        String telemetryKey,
        String telemetryValue,
        Map<String, String> responseHeaders) {}
