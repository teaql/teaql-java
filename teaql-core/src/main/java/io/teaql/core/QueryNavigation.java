package io.teaql.core;

import io.teaql.core.criteria.AND;
import io.teaql.core.criteria.OR;
import io.teaql.core.criteria.NOT;
import io.teaql.core.criteria.IsNotNull;
import java.util.ArrayList;

/** Pure builder rewrite. A null result means TRUE in the new joined population. */
final class QueryNavigation {
    private QueryNavigation() {}

    static SearchCriteria clear(SearchCriteria criteria, String relation, int depth) {
        if (depth > 64) throw new TeaQLRuntimeException("Roll-up navigation expression exceeds 64 levels");
        if (criteria instanceof SubQuerySearchCriteria membership && membership.navigationOwned()
                && relation.equals(membership.getPropertyName())) return null;
        if (criteria != null && (criteria.getClass() == AND.class || criteria.getClass() == OR.class)) {
            boolean and = criteria.getClass() == AND.class;
            var result = new ArrayList<SearchCriteria>();
            for (var operand : ((FunctionApply) criteria).getExpressions()) {
                if (!(operand instanceof SearchCriteria condition))
                    throw new TeaQLRuntimeException("Roll-up navigation Boolean operands must be conditions");
                var rewritten = clear(condition, relation, depth + 1);
                if (rewritten != null) result.add(rewritten);
                else if (!and) result.add(new IsNotNull(new PropertyReference(BaseEntity.ID_PROPERTY)));
            }
            if (and && result.isEmpty()) return null;
            if (result.size() == 1) return result.get(0);
            return and ? SearchCriteria.and(result.toArray(SearchCriteria[]::new)) : SearchCriteria.or(result.toArray(SearchCriteria[]::new));
        }
        if (criteria != null && criteria.getClass() == NOT.class) {
            var operands = ((FunctionApply) criteria).getExpressions();
            if (operands.size() != 1 || !(operands.get(0) instanceof SearchCriteria condition))
                throw new TeaQLRuntimeException("Roll-up navigation NOT requires one condition");
            var rewritten = clear(condition, relation, depth + 1);
            return SearchCriteria.not(rewritten == null
                    ? new IsNotNull(new PropertyReference(BaseEntity.ID_PROPERTY)) : rewritten);
        }
        return criteria;
    }
}
