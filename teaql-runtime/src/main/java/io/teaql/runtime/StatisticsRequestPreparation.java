package io.teaql.runtime;

import io.teaql.core.*;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;

/** Authorize typed builders before freezing one invocation's statistical tree. */
final class StatisticsRequestPreparation {
    private final QueryPolicy policy;
    private final IdentityHashMap<SearchRequest<?>, Boolean> active = new IdentityHashMap<>();
    private int layers;

    StatisticsRequestPreparation(QueryPolicy policy) { this.policy = policy; }

    @SuppressWarnings({"rawtypes", "unchecked"})
    <T extends Entity> SearchRequest<T> prepare(UserContext context, SearchRequest<T> request,
            QueryRequestScope scope) {
        if (++layers > 64 || active.put(request, Boolean.TRUE) != null) {
            throw new TeaQLRuntimeException("Cyclic statistics or statistical plan exceeds 64 layers");
        }
        try {
            // Preserve application policies that use generated typed methods.
            // The immutable intent header is captured before this mutable hook.
            if (policy != null) policy.enforceSelect(context, request);
            var children = new LinkedHashMap<String, SearchRequest>();
            for (var entry : request.getPropagateDimensions().entrySet()) {
                String relation = entry.getKey();
                SearchRequest<?> child = entry.getValue();
                var operation = scope.deriveOperation(child.getTypeName(),
                        QueryOperation.framework(QueryOperation.Kind.AGGREGATE, null, relation));
                var childScope = operation.deriveChild(child.getTypeName(), child.comment());
                children.put(relation, prepare(context, child, childScope));
            }
            var snapshot = new QueryRequestSnapshot<T>(request, scope);
            snapshot.setPropagateDimensions(children);
            return snapshot;
        } finally { active.remove(request); }
    }
}
