package io.teaql.core.i18n;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;

/** Shared immutable runtime vocabulary, loaded once during composition, never while rendering. */
public final class CheckerMessageDefaults {
    private static final Map<Locale, Map<String, String>> VALUES = load();
    private static final Map<Locale, String> CURRENT = currentObjects();
    private CheckerMessageDefaults() {}

    public static Map<Locale, String> currentObjectLabels() { return CURRENT; }

    /** Select the unit actually measured by a Checker, not a conversion of that measurement. */
    public static String unit(Locale locale, String countingUnit) {
        String value = VALUES.get(locale).get(countingUnit);
        if (value == null || "currentObject".equals(countingUnit)) throw new IllegalArgumentException("Unsupported Checker counting unit");
        return value;
    }

    private static Map<Locale, String> currentObjects() {
        Map<Locale, String> result = new EnumMap<>(Locale.class);
        VALUES.forEach((locale, labels) -> result.put(locale, labels.get("currentObject")));
        return Map.copyOf(result);
    }

    private static Map<Locale, Map<String, String>> load() {
        try (var input = CheckerMessageDefaults.class.getResourceAsStream("checker-presentation-v1.json")) {
            if (input == null) throw new IllegalArgumentException("Missing Checker presentation defaults");
            var root = new ObjectMapper().readTree(input);
            if (!"teaql.checker-presentation/v1".equals(root.path("schema").asText()))
                throw new IllegalArgumentException("Unsupported Checker presentation defaults");
            Map<Locale, Map<String, String>> result = new EnumMap<>(Locale.class);
            root.path("locales").fields().forEachRemaining(entry -> {
                Map<String, String> labels = new HashMap<>();
                for (String key : List.of("currentObject", "utf16", "scalar", "byte")) {
                    var value = entry.getValue().path(key);
                    if (!value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException("Incomplete Checker presentation defaults");
                    labels.put(key, value.textValue());
                }
                if (result.put(Locale.fromCode(entry.getKey()), Map.copyOf(labels)) != null)
                    throw new IllegalArgumentException("Duplicate Checker presentation Locale");
            });
            if (result.size() != Locale.values().length) throw new IllegalArgumentException("Incomplete Checker presentation Locale set");
            return Map.copyOf(result);
        } catch (IOException exception) { throw new ExceptionInInitializerError("Cannot load Checker presentation defaults"); }
    }
}
