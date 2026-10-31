package io.teaql.core.utils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** One shared audit/diagnostic policy, independent of runtime and providers. */
public final class SensitiveValueProjection {
    public static final String ENVIRONMENT = "TEAQL_ALLOW_SENSITIVE_PLAINTEXT_LOGS";
    public static final String ACKNOWLEDGEMENT = "I_UNDERSTAND_SENSITIVE_DATA_MAY_BE_WRITTEN_TO_DISK";
    public static final String REDACTED = "[REDACTED]";
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private SensitiveValueProjection() {}

    public static boolean plaintextEnabled() {
        if (!ACKNOWLEDGEMENT.equals(System.getenv(ENVIRONMENT))) return false;
        if (WARNED.compareAndSet(false,true)) System.err.println(
            "[TeaQL WARNING] Sensitive plaintext logging enabled; business data may be written to disk. Credentials remain redacted.");
        return true;
    }
    public static String maskAuditValue(String value) {
        if (value == null || value.isEmpty()) return value;
        int length = value.codePointCount(0,value.length());
        if (length < 8 || value.codePoints().allMatch(c -> c >= '0' && c <= '9')) return "*".repeat(length);
        return value.substring(0,value.offsetByCodePoints(0,2)) + "*".repeat(length-4)
            + value.substring(value.offsetByCodePoints(0,length-2));
    }
    public static boolean hasCredentials(Object value) {
        if (value instanceof Map<?,?> map) return map.entrySet().stream()
            .anyMatch(e -> SensitiveLogNames.credential(String.valueOf(e.getKey())) || hasCredentials(e.getValue()));
        if (value instanceof Iterable<?> items) for (Object item : items) if (hasCredentials(item)) return true;
        if (value != null && value.getClass().isArray())
            for (int i=0;i<java.lang.reflect.Array.getLength(value);i++)
                if (hasCredentials(java.lang.reflect.Array.get(value,i))) return true;
        return false;
    }
    public static void collectStrings(Object value, Collection<String> output) {
        if (value == null) return;
        if (value instanceof Map<?,?> map) { map.values().forEach(v -> collectStrings(v,output)); return; }
        if (value instanceof Iterable<?> items) { items.forEach(v -> collectStrings(v,output)); return; }
        if (value.getClass().isArray()) {
            for (int i=0;i<java.lang.reflect.Array.getLength(value);i++) collectStrings(java.lang.reflect.Array.get(value,i),output);
            return;
        }
        String text=String.valueOf(value); if (!text.isEmpty()) output.add(text);
    }
    public static String scrub(String text, Collection<?> values) {
        if (text == null) return null;
        List<String> strings=new ArrayList<>(); values.forEach(v -> collectStrings(v,strings));
        strings.sort(Comparator.comparingInt(String::length).reversed());
        for (String value : strings) text=text.replace(value,REDACTED);
        return text;
    }
    public static Object maskBusinessValue(Object value) {
        if (value == null || REDACTED.equals(value)) return value;
        if (value instanceof Map<?,?>) return REDACTED;
        if (value instanceof Iterable<?> items) {
            List<Object> safe=new ArrayList<>(); items.forEach(v -> safe.add(maskBusinessValue(v))); return safe;
        }
        if (value.getClass().isArray()) {
            List<Object> safe=new ArrayList<>();
            for (int i=0;i<java.lang.reflect.Array.getLength(value);i++) safe.add(maskBusinessValue(java.lang.reflect.Array.get(value,i)));
            return safe;
        }
        return maskAuditValue(String.valueOf(value));
    }
}
