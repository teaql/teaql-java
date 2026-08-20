package io.teaql.core.i18n;

import java.util.Map;

public enum Locale {
    ENGLISH("en"), CHINESE_SIMPLIFIED("zh-CN"), CHINESE_TRADITIONAL("zh-TW"),
    JAPANESE("ja"), KOREAN("ko"), GERMAN("de"), FRENCH("fr"), SPANISH("es"),
    PORTUGUESE("pt"), ARABIC("ar"), THAI("th"), INDONESIAN("id"), FILIPINO("fil"),
    UKRAINIAN("uk"), VIETNAMESE("vi");

    private final String code;
    Locale(String code) { this.code = code; }
    public String code() { return code; }

    private static final Map<String, Locale> ALIASES = Map.ofEntries(
        Map.entry("en-us", ENGLISH), Map.entry("en-gb", ENGLISH),
        Map.entry("zh", CHINESE_SIMPLIFIED), Map.entry("zh-hans", CHINESE_SIMPLIFIED),
        Map.entry("zh-sg", CHINESE_SIMPLIFIED), Map.entry("cn", CHINESE_SIMPLIFIED),
        Map.entry("zh-hant", CHINESE_TRADITIONAL), Map.entry("zh-hk", CHINESE_TRADITIONAL),
        Map.entry("zh-mo", CHINESE_TRADITIONAL), Map.entry("tw", CHINESE_TRADITIONAL),
        Map.entry("ja-jp", JAPANESE), Map.entry("ko-kr", KOREAN), Map.entry("de-de", GERMAN),
        Map.entry("fr-fr", FRENCH), Map.entry("es-mx", SPANISH), Map.entry("pt-br", PORTUGUESE),
        Map.entry("pt-pt", PORTUGUESE), Map.entry("ar-sa", ARABIC), Map.entry("th-th", THAI),
        Map.entry("id-id", INDONESIAN), Map.entry("tl", FILIPINO), Map.entry("fil-ph", FILIPINO),
        Map.entry("uk-ua", UKRAINIAN), Map.entry("vi-vn", VIETNAMESE));

    public static Locale fromCode(String code) {
        if (code == null || code.trim().isEmpty()) throw new UnsupportedLocaleException(code);
        String normalized = code.trim().replace('_', '-').toLowerCase(java.util.Locale.ROOT);
        for (Locale value : values()) if (value.code.toLowerCase(java.util.Locale.ROOT).equals(normalized)) return value;
        Locale alias = ALIASES.get(normalized);
        if (alias == null) throw new UnsupportedLocaleException(code);
        return alias;
    }
}
