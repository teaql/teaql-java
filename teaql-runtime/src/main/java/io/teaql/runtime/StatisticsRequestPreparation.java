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
            snapshot.replaceSearchCriteria(populationCriteria(request.getSearchCriteria(), request));
            snapshot.setPropagateDimensions(children);
            return snapshot;
        } finally { active.remove(request); }
    }

    /**
     * Generated rollUpTo binds both an identity-membership predicate and a
     * dimension edge to the same parent builder. The statistical join already
     * enforces that parent's eligibility and cell bounds. Materializing the
     * grouped parent as entity IDs would instead lose its source population.
     * Normalize only positive conjuncts in this invocation, never the builder
     * or an independent/negated/disjunctive business predicate.
     */
    private SearchCriteria populationCriteria(SearchCriteria criteria, SearchRequest<?> owner) {
        if (criteria instanceof SubQuerySearchCriteria membership
                && BaseEntity.ID_PROPERTY.equals(membership.getDependsOnPropertyName())
                && owner.getPropagateDimensions().get(membership.getPropertyName()) == membership.getDependsOn()) return null;
        if (criteria != null && criteria.getClass() == io.teaql.core.criteria.AND.class) {
            var remaining = new java.util.ArrayList<SearchCriteria>();
            for (var operand : ((io.teaql.core.criteria.AND) criteria).getExpressions()) {
                if (!(operand instanceof SearchCriteria condition))
                    throw new TeaQLRuntimeException("Statistical source AND requires conditions");
                var normalized = populationCriteria(condition, owner);
                if (normalized != null) remaining.add(normalized);
            }
            if (remaining.isEmpty()) return null;
            if (remaining.size() == 1) return remaining.get(0);
            return SearchCriteria.and(remaining.toArray(SearchCriteria[]::new));
        }
        return criteria;
    }
}
