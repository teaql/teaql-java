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
            snapshot.replaceSearchCriteria(populationCriteria(snapshot.getSearchCriteria(), snapshot, 0));
            snapshot.setPropagateDimensions(children);
            return snapshot;
        } finally { active.remove(request); }
    }

    /**
     * Generated rollUpTo binds both an identity-membership predicate and a
     * dimension edge to the same parent builder. The statistical join already
     * enforces that parent's eligibility and cell bounds. Materializing the
     * grouped parent as entity IDs would instead lose its source population.
     * A reference to that exact parent is true within this join's population,
     * including inside OR/NOT. Independent business subqueries are not edges.
     * Preserve other branches for normal metadata/policy validation, even when
     * a linked membership makes an OR logically true. Never rewrite a builder.
     */
    private SearchCriteria populationCriteria(SearchCriteria criteria, SearchRequest<?> owner, int depth) {
        if (depth > 64) throw new TeaQLRuntimeException("Statistical membership expression exceeds 64 levels");
        if (criteria instanceof SubQuerySearchCriteria membership
                && BaseEntity.ID_PROPERTY.equals(membership.getDependsOnPropertyName())
                && owner.getPropagateDimensions().get(membership.getPropertyName()) == membership.getDependsOn()) return null;
        if (criteria != null && (criteria.getClass() == io.teaql.core.criteria.AND.class
                || criteria.getClass() == io.teaql.core.criteria.OR.class)) {
            boolean conjunction = criteria.getClass() == io.teaql.core.criteria.AND.class;
            var remaining = new java.util.ArrayList<SearchCriteria>();
            for (var operand : ((FunctionApply) criteria).getExpressions()) {
                if (!(operand instanceof SearchCriteria condition))
                    throw new TeaQLRuntimeException("Statistical source Boolean expression requires conditions");
                var normalized = populationCriteria(condition, owner, depth + 1);
                if (normalized == null && !conjunction) normalized = identityPresent();
                if (normalized != null) remaining.add(normalized);
            }
            if (remaining.isEmpty()) return null;
            if (remaining.size() == 1) return remaining.get(0);
            return conjunction ? SearchCriteria.and(remaining.toArray(SearchCriteria[]::new))
                    : SearchCriteria.or(remaining.toArray(SearchCriteria[]::new));
        }
        if (criteria != null && criteria.getClass() == io.teaql.core.criteria.NOT.class) {
            var function = (FunctionApply) criteria;
            if (function.getExpressions().size() != 1 || !(function.first() instanceof SearchCriteria condition))
                throw new TeaQLRuntimeException("Statistical source NOT requires one condition");
            var normalized = populationCriteria(condition, owner, depth + 1);
            // A persistent observation has a non-null identity. This expresses
            // false using the existing typed AST, not raw SQL or bind payloads.
            return normalized == null ? new io.teaql.core.criteria.IsNull(new PropertyReference(BaseEntity.ID_PROPERTY))
                    : SearchCriteria.not(normalized);
        }
        return criteria;
    }

    private static SearchCriteria identityPresent() {
        return new io.teaql.core.criteria.IsNotNull(new PropertyReference(BaseEntity.ID_PROPERTY));
    }
}
