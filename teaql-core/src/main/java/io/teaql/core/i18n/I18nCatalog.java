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
        String key = switch (result.getRuleId()) {
            case REQUIRED -> "checker.required"; case MIN -> "checker.min"; case MAX -> "checker.max";
            case MIN_STR_LEN -> "checker.minLength"; case MAX_STR_LEN -> "checker.maxLength";
            default -> "checker." + result.getRuleId().name().toLowerCase(java.util.Locale.ROOT);
        };
        String input = String.valueOf(result.getInputValue());
        return message(locale, key)
            .replace("{location}", String.valueOf(result.getLocation()))
            .replace("{system}", String.valueOf(result.getSystemValue()))
            .replace("{input}", input)
            .replace("{input_len}", result.getInputValue() instanceof CharSequence cs ? String.valueOf(cs.length()) : "0");
    }

    private static I18nCatalog loadBuiltin() {
        InputStream input = I18nCatalog.class.getResourceAsStream("builtin-messages-v1.json");
        if (input == null) throw new ExceptionInInitializerError("Missing built-in i18n catalog");
        return fromJson(input, null);
    }
}
