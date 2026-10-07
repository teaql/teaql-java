package io.teaql.core.checker.internal;

import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.checker.FixEvidence;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Framework-owned, synchronous check-and-fix invocation. This is NOT a trace
 * scope, graph ledger, or asynchronous context-propagation mechanism.
 *
 * <p>The thread binding only adapts existing Checker callbacks that receive the
 * original UserContext. All temporary data belongs to this invocation, never
 * to that Context's shared attribute map. A nested save opens its own invocation
 * and close restores the parent. No scope may cross a thread/async boundary.
 */
public final class CheckerInvocation implements AutoCloseable {
    private static final ThreadLocal<CheckerInvocation> ACTIVE = new ThreadLocal<>();
    // One bounded, value-free diagnostic receipt per thread, not a Context-keyed
    // map retaining every Context or graph ever checked by a worker.
    private static final ThreadLocal<Completed> LAST = new ThreadLocal<>();
    private record Completed(WeakReference<UserContext> context, List<FixEvidence> evidence) {}

    private final UserContext context;
    private final CheckerInvocation parent;
    private final boolean mutationOnly;
    private final Map<String, Object> attributes = new HashMap<>();
    private boolean closed;

    private CheckerInvocation(UserContext context, boolean mutationOnly) {
        this.context = java.util.Objects.requireNonNull(context, "context");
        this.parent = ACTIVE.get();
        this.mutationOnly = mutationOnly;
        attributes.put(Checker.TEAQL_DATA_CHECK_RESULT, new ArrayList<CheckResult>());
        attributes.put(Checker.TEAQL_DATA_CHECKED_ITEMS, new ArrayList<>());
        attributes.put(UserContext.TEAQL_FIX_EVIDENCE_CURRENT, new ArrayList<FixEvidence>());
        // Each independent graph captures its own context-provided time once.
        attributes.put(Checker.TEAQL_FIX_TIME, context.businessTime());
        ACTIVE.set(this);
    }

    /** Internal synchronous runtime boundary; do not open scopes in business code. */
    public static CheckerInvocation open(UserContext context) {
        return new CheckerInvocation(context, false);
    }

    /** Save validates changed objects, not untouched partial reference details. */
    public static CheckerInvocation openMutation(UserContext context) {
        return new CheckerInvocation(context, true);
    }

    public boolean mutationOnly() {
        return mutationOnly;
    }

    public static CheckerInvocation current(UserContext context) {
        for (CheckerInvocation scope = ACTIVE.get(); scope != null; scope = scope.parent) {
            if (scope.context == context) return scope;
        }
        return null;
    }

    public static boolean isScopedAttribute(String key) {
        return Checker.TEAQL_DATA_CHECK_RESULT.equals(key)
                || Checker.TEAQL_DATA_CHECKED_ITEMS.equals(key)
                || Checker.TEAQL_FIX_TIME.equals(key)
                || UserContext.TEAQL_FIX_EVIDENCE_CURRENT.equals(key)
                || UserContext.TEAQL_FIX_EVIDENCE_LAST.equals(key);
    }

    public Object attribute(String key) { return attributes.get(key); }

    public void attribute(String key, Object value) {
        if (!isScopedAttribute(key)) throw new IllegalArgumentException("Not a Checker invocation attribute");
        if (value == null) attributes.remove(key);
        else attributes.put(key, value);
    }

    /** Adapts Checker helpers even when an application supplies its own UserContext implementation. */
    public static Object attribute(UserContext context, String key) {
        CheckerInvocation scope = current(context);
        return scope == null ? context.getAttribute(key) : scope.attribute(key);
    }

    public static void attribute(UserContext context, String key, Object value) {
        CheckerInvocation scope = current(context);
        if (scope == null) context.putAttribute(key, value);
        else scope.attribute(key, value);
    }

    /** Last completed check on this synchronous execution thread; null means no receipt for this Context. */
    public static List<FixEvidence> lastEvidence(UserContext context) {
        Completed completed = LAST.get();
        return completed != null && completed.context().get() == context ? completed.evidence() : null;
    }

    public static void forgetLastEvidence(UserContext context) {
        Completed completed = LAST.get();
        if (completed != null && completed.context().get() == context) LAST.remove();
    }

    @Override @SuppressWarnings("unchecked")
    public void close() {
        if (closed) return;
        if (ACTIVE.get() != this) throw new IllegalStateException("Checker invocations must close on their owning thread in reverse order");
        try {
            List<FixEvidence> evidence = (List<FixEvidence>) attributes.get(UserContext.TEAQL_FIX_EVIDENCE_LAST);
            if (evidence == null) evidence = (List<FixEvidence>) attributes.get(UserContext.TEAQL_FIX_EVIDENCE_CURRENT);
            LAST.set(new Completed(new WeakReference<>(context), evidence == null ? List.of() : List.copyOf(evidence)));
        } finally {
            // Diagnostic copy/cast failure must not strand this invocation or
            // retain the graph after a Checker exception on a reusable worker.
            if (parent == null) ACTIVE.remove();
            else ACTIVE.set(parent);
            attributes.clear();
            closed = true;
        }
    }
}
