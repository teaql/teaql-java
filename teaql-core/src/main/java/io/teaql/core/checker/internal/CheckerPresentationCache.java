package io.teaql.core.checker.internal;

import io.teaql.core.checker.CheckResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Safe presentation provenance only: no raw message, input, entity or Context. */
final class CheckerPresentationCache {
    private record Entry(int length, byte[] messageDigest, byte[] inputDigest) {}
    private static final Map<CheckResult,Entry> GENERATED=Collections.synchronizedMap(new WeakHashMap<>());
    private CheckerPresentationCache() {}

    static Integer validationLength(CheckResult result) {
        Entry entry=automaticEntry(result);
        if (entry == null) return null;
        if (Arrays.equals(entry.inputDigest(),inputDigest(result.getInputValue()))) return entry.length();
        // A caller explicitly replaced the input. Do not reuse a stale length.
        return result.getInputValue() instanceof CharSequence text ? text.length() : 0;
    }

    static boolean isAutomatic(CheckResult result) { return automaticEntry(result) != null; }

    static void remember(CheckResult result,int validationLength) {
        GENERATED.put(result,new Entry(validationLength,digest(result.getNaturalLanguageStatement()),inputDigest(result.getInputValue())));
    }

    static void forget(CheckResult result) { GENERATED.remove(result); }

    private static Entry automaticEntry(CheckResult result) {
        Entry entry=GENERATED.get(result);String message=result.getNaturalLanguageStatement();
        return entry != null && message != null && MessageDigest.isEqual(entry.messageDigest(),digest(message)) ? entry : null;
    }

    private static byte[] inputDigest(Object value) {
        return value instanceof CharSequence text ? digest(text.toString()) : null;
    }

    private static byte[] digest(String text) {
        try { return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 is required for safe Checker presentation",error); }
    }
}
