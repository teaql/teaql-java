package io.teaql.core.checker;

/** Model-selected naming policy for JSON fields and RFC 6901 instance paths. */
public enum JsonFieldNamingProfile {
    CAMEL_CASE("camelCase"),
    SNAKE_CASE("snake_case"),
    PASCAL_CASE("PascalCase");

    private final String modelValue;

    JsonFieldNamingProfile(String pModelValue) {
        modelValue = pModelValue;
    }

    public String modelValue() {
        return modelValue;
    }

    public String render(String canonicalName) {
        if (this == SNAKE_CASE) {
            return canonicalName;
        }
        String[] parts = canonicalName.split("_", -1);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (i == 0 && this == CAMEL_CASE) {
                result.append(parts[i]);
            } else {
                result.append(Character.toUpperCase(parts[i].charAt(0)))
                        .append(parts[i].substring(1));
            }
        }
        return result.toString();
    }

    public static JsonFieldNamingProfile fromModelValue(String value) {
        if (value == null || value.isBlank() || "camelCase".equals(value)) {
            return CAMEL_CASE;
        }
        for (JsonFieldNamingProfile profile : values()) {
            if (profile.modelValue.equals(value)) return profile;
        }
        throw new IllegalArgumentException("Unsupported json_field_naming: " + value);
    }
}
