package io.teaql.core;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.teaql.core.utils.SensitiveLogNames;
import java.util.*;

/** Invocation-local provider/runtime provenance; never retained by a projected log. */
@JsonIgnoreType
public final class SqlIntentRedactions {
    private record Secret(String value, boolean forced) {}
    private final List<Secret> secrets = new ArrayList<>();

    @FrameworkInternal("Independent nested-query provenance snapshot")
    public SqlIntentRedactions copy() {
        var result = new SqlIntentRedactions();
        result.secrets.addAll(secrets);
        return result;
    }

    @FrameworkInternal("Merge captured strings, never shared mutable entity state")
    public void include(SqlIntentRedactions source) {
        if (source != null) secrets.addAll(source.secrets);
    }

    @FrameworkInternal("Capture loaded and changed prior scalar values before graph writes")
    public void captureEntity(BaseEntity entity, io.teaql.core.meta.EntityDescriptor descriptor) {
        for (var current = descriptor; current != null; current = current.getParent()) {
            for (var property : current.getOwnProperties()) {
                if (property instanceof io.teaql.core.meta.Relation) continue;
                String name = property.getName();
                var policy = List.of(SqlFieldLogPolicy.resolve(descriptor, property));
                if (entity.isPropertyLoaded(name)) capture(policy, new Object[]{entity.getProperty(name)});
                if (entity.getUpdatedProperties().contains(name)) capture(policy, new Object[]{entity.getOldValue(name)});
            }
        }
    }

    @FrameworkInternal("SQL diagnostic provenance only; not an application policy")
    public void capture(List<SqlParameterLogPolicy> policies, Object[] values) {
        boolean invalid = policies.size() != values.length;
        for (int i = 0; i < values.length; i++) {
            var policy = invalid ? SqlParameterLogPolicy.UNKNOWN : policies.get(i);
            boolean forced = policy == SqlParameterLogPolicy.UNKNOWN || policy == SqlParameterLogPolicy.CREDENTIAL
                    || hasCredentials(values[i]);
            if (forced || policy == SqlParameterLogPolicy.MASKED) collect(values[i], forced);
        }
    }

    /** Keep a mutation's plain ID out of diagnostic prose without changing its SQL binding policy. */
    @FrameworkInternal("Invocation-local SQL intent provenance only")
    public void captureTargetId(Object id) {
        collect(id, true);
    }

    private static boolean hasCredentials(Object value) {
        if (value instanceof Map<?, ?> map) return map.entrySet().stream()
                .anyMatch(e -> SensitiveLogNames.credential(String.valueOf(e.getKey())) || hasCredentials(e.getValue()));
        if (value instanceof Iterable<?> items) for (Object item : items) if (hasCredentials(item)) return true;
        if (value != null && value.getClass().isArray())
            for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++)
                if (hasCredentials(java.lang.reflect.Array.get(value, i))) return true;
        return false;
    }

    private void collect(Object value, boolean forced) {
        if (value == null) return;
        if (value instanceof Map<?, ?> map) { map.values().forEach(v -> collect(v, forced)); return; }
        if (value instanceof Iterable<?> items) { items.forEach(v -> collect(v, forced)); return; }
        if (value.getClass().isArray()) {
            for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++)
                collect(java.lang.reflect.Array.get(value, i), forced);
            return;
        }
        String text = String.valueOf(value);
        if (!text.isEmpty()) secrets.add(new Secret(text, forced));
    }

    @FrameworkInternal("Runtime log projection only")
    public void appendTo(List<Object> output, boolean allowPlaintext) {
        for (var secret : secrets) if (secret.forced() || !allowPlaintext) output.add(secret.value());
    }

    @Override public String toString() { return "SqlIntentRedactions[count=" + secrets.size() + "]"; }
}
