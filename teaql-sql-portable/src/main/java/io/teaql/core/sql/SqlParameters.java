package io.teaql.core.sql;

import io.teaql.core.SqlParameterLogPolicy;
import java.util.HashMap;
import java.util.Map;

/** Per-compilation bindings; never stored on UserContext or a shared dialect instance. */
public final class SqlParameters extends HashMap<String, Object> {
    private final Map<String, SqlParameterLogPolicy> policies = new HashMap<>();
    private SqlParameterLogPolicy currentPolicy = SqlParameterLogPolicy.UNKNOWN;
    private boolean generated = true;
    private io.teaql.core.QueryIntent queryIntent;
    private io.teaql.core.SearchRequest<?> originatingQuery;

    /** Immutable originating intent for cross-provider relation predicates during this compilation. */
    public io.teaql.core.QueryIntent queryIntent() { return queryIntent; }
    public void captureQueryIntent(io.teaql.core.QueryIntent intent) {
        if (queryIntent == null) queryIntent = java.util.Objects.requireNonNull(intent, "intent");
    }
    /** Provider-owned scoped request, retained only for this compilation. */
    public io.teaql.core.SearchRequest<?> originatingQuery() { return originatingQuery; }
    public void captureQueryContext(io.teaql.core.SearchRequest<?> request) {
        var intent = request.inheritedQueryIntent();
        if (intent == null && request.comment() != null && request.purpose() != null) {
            intent = io.teaql.core.QueryIntent.of(request.comment(), request.purpose());
        }
        if (intent != null) {
            captureQueryIntent(intent);
            if (originatingQuery == null) originatingQuery = request;
        }
    }

    public SqlParameterLogPolicy policy(String name) {
        return policies.getOrDefault(name, SqlParameterLogPolicy.UNKNOWN);
    }
    public SqlParameterLogPolicy currentPolicy() { return currentPolicy; }
    public void currentPolicy(SqlParameterLogPolicy policy) { currentPolicy = policy; }
    public boolean generated() { return generated; }
    public void untrusted() { generated = false; }

    @Override public Object put(String name, Object value) {
        policies.remove(name);
        return super.put(name, value);
    }
    @Override public void clear() { policies.clear(); super.clear(); }
    @Override public Object remove(Object key) { policies.remove(key); return super.remove(key); }
    @Override public void putAll(Map<? extends String, ?> values) { values.forEach(this::put); }

    public static void bind(Map<String, Object> parameters, String name, Object value,
                            SqlParameterLogPolicy policy) {
        parameters.put(name, value);
        if (parameters instanceof SqlParameters tracked) tracked.policies.put(name, policy);
    }
}
