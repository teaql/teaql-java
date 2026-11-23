package io.teaql.core.i18n;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.checker.CheckResult;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class I18nCatalog {
    private static final I18nCatalog BUILTIN = loadBuiltin();
    private final Map<String, Map<String, String>> messages;
    private final I18nCatalog fallback;

    private I18nCatalog(Map<String, Map<String, String>> messages, I18nCatalog fallback) {
        this.messages = messages; this.fallback = fallback;
    }

    public static I18nCatalog builtin() { return BUILTIN; }

    public static I18nCatalog fromJson(InputStream input) {
        return fromJson(input, BUILTIN);
    }

    public static I18nCatalog fromJson(InputStream input, I18nCatalog fallback) {
        try {
            JsonNode root = new ObjectMapper().readTree(input);
            if (!"teaql.i18n/v1".equals(root.path("schema").asText())) throw new IllegalArgumentException("Unsupported i18n schema");
            Map<String, Map<String, String>> all = new HashMap<>();
            root.path("locales").fields().forEachRemaining(locale -> {
                Locale canonical = Locale.fromCode(locale.getKey());
                Map<String, String> values = new HashMap<>();
                locale.getValue().path("messages").fields().forEachRemaining(e -> values.put(e.getKey(), e.getValue().asText()));
                all.put(canonical.code(), values);
            });
            return new I18nCatalog(all, fallback);
        } catch (IOException e) { throw new IllegalArgumentException("Invalid i18n catalog", e); }
    }

    public String message(Locale locale, String key) {
        String value = messages.getOrDefault(locale.code(), Map.of()).get(key);
        if (value == null && fallback != null) value = fallback.messages.getOrDefault(locale.code(), Map.of()).get(key);
        if (value == null) value = messages.getOrDefault("en", Map.of()).get(key);
        if (value == null && fallback != null) value = fallback.messages.getOrDefault("en", Map.of()).get(key);
        return value == null ? key : value;
    }

    public String render(CheckResult result, Locale locale) {
        return render(result, locale, result.getInputValue() instanceof CharSequence cs ? cs.length() : 0);
    }

    /** Render projected input while retaining the unit used by the actual validation. */
    public String render(CheckResult result, Locale locale, int originalInputLength) {
        String key = messageKey(result);
        String input = String.valueOf(result.getInputValue());
        return message(locale, key)
            .replace("{location}", String.valueOf(result.getLocation()))
            .replace("{system}", String.valueOf(result.getSystemValue()))
            .replace("{input}", input)
            .replace("{input_len}", String.valueOf(originalInputLength));
    }

    public static String messageKey(CheckResult result) {
        if (result.getRuleId() == null) return "checker.invalid";
        return switch (result.getRuleId()) {
            case REQUIRED -> "checker.required"; case MIN -> "checker.min"; case MAX -> "checker.max";
            case MIN_STR_LEN -> "checker.minLength"; case MAX_STR_LEN -> "checker.maxLength";
            default -> "checker." + result.getRuleId().name().toLowerCase(java.util.Locale.ROOT);
        };
    }

    /** Exact Locale, explicit application text only. Do not mistake historical built-ins for overrides. */
    public String applicationMessage(Locale locale, String key) {
        for (I18nCatalog current = this; current != null && current != BUILTIN; current = current.fallback) {
            String value = current.messages.getOrDefault(locale.code(), Map.of()).get(key);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    /** Single-pass compatibility interpolation: keep unknown legacy braces literal, never reparse inserted data. */
    public static String renderApplicationMessage(String template, Map<String, String> arguments) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < template.length();) {
            if (template.charAt(i) == '{') {
                String key = template.startsWith("{location}", i) ? "location"
                        : template.startsWith("{system}", i) ? "system"
                        : template.startsWith("{input}", i) ? "input"
                        : template.startsWith("{input_len}", i) ? "input_len" : null;
                String value = key == null ? null : arguments.get(key);
                if (value != null) { result.append(value); i += key.length() + 2; continue; }
            }
            result.append(template.charAt(i++));
        }
        return result.toString();
    }

    private static I18nCatalog loadBuiltin() {
        InputStream input = I18nCatalog.class.getResourceAsStream("builtin-messages-v1.json");
        if (input == null) throw new ExceptionInInitializerError("Missing built-in i18n catalog");
        return fromJson(input, null);
    }
}
