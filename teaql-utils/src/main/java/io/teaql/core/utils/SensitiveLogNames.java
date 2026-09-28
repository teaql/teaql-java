package io.teaql.core.utils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Shared conservative credential classification for compilers and log projections. */
public final class SensitiveLogNames {
    private SensitiveLogNames() {}

    public static boolean credential(String name) {
        String key = Objects.toString(name, "").replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        return List.of("password", "passwd", "passphrase", "privatekey", "secret", "accesstoken",
                "refreshtoken", "idtoken", "apikey", "authorization", "credential", "sessiontoken", "magiclinktoken")
                .stream().anyMatch(key::contains);
    }
}
