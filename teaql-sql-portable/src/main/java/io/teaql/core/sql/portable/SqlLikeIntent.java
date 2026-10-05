package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.criteria.*;
import io.teaql.core.sql.expression.ExpressionHelper;
import java.util.*;

/** Read-only provenance from typed operands before any root or derived SQL executes. */
final class SqlLikeIntent {
    private final UserContext context;
    private final SqlIntentRedactions output;
    private final Set<SearchRequest<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());

    private SqlLikeIntent(UserContext context, SqlIntentRedactions output) {
        this.context = context;
        this.output = output;
    }

    static void capture(UserContext context, SearchRequest<?> request,
            PortableSQLRepository<?> repository, SqlIntentRedactions output) {
        if (output != null) new SqlLikeIntent(context, output).request(request, repository);
    }

    private void request(SearchRequest<?> request, PortableSQLRepository<?> repository) {
        if (request == null || repository == null || !visited.add(request)) return;
        expression(request.getSearchCriteria(), repository);
        if (request.getProjections() != null)
            for (var projection : request.getProjections()) expression(projection.getExpression(), repository);
        if (request.getOrderBy() != null)
            for (var order : request.getOrderBy().getOrderBys()) expression(order.getExpression(), repository);
        if (request.getAggregations() != null)
            for (var selected : request.getAggregations().getSelectedExpressions()) expression(selected.getExpression(), repository);
        if (request.enhanceRelations() != null)
            for (var child : request.enhanceRelations().values()) child(child, repository);
        if (request.enhanceChildren() != null)
            for (var child : request.enhanceChildren().values()) child(child, repository);
        if (request.getFacetRequests() != null)
            for (var facet : request.getFacetRequests()) child(facet.getRequest(), repository);
        if (request.getDynamicAggregateAttributes() != null)
            for (var aggregate : request.getDynamicAggregateAttributes()) child(aggregate.getAggregateRequest(), repository);
    }

    private void child(SearchRequest<?> child, PortableSQLRepository<?> owner) {
        if (child == null || owner.getResolver() == null) return;
        request(child, owner.getResolver().resolve(child.getTypeName()));
    }

    private void expression(Expression expression, PortableSQLRepository<?> repository) {
        if (!ExpressionHelper.hasBuiltinParser(expression, repository)) return;
        if (expression instanceof VersionSearchCriteria version) {
            expression(version.getSearchCriteria(), repository);
        } else if (expression instanceof SubQuerySearchCriteria subquery) {
            child(subquery.getDependsOn(), repository);
        } else if (expression instanceof FunctionApply function) {
            if (function instanceof Between && function.getOperator() == Operator.BETWEEN
                    && function.getExpressions().size() == 3
                    && ExpressionHelper.hasBuiltinParser(function.first(), repository)) {
                // Each bound may have been rewritten independently; trust only matching typed operands.
                capture(function, function.second(), Operator.BETWEEN, repository);
                capture(function, function.third(), Operator.BETWEEN, repository);
            } else if (function instanceof TwoOperatorCriteria && function.getExpressions().size() == 2
                    && function.getOperator() instanceof Operator operator) {
                capture(function, function.second(), operator, repository);
                if (operator == Operator.EQUAL
                        && phonetic(function.first(), repository) instanceof PropertyReference
                        && phonetic(function.second(), repository) instanceof Parameter parameter) {
                    // BaseRequest emits EQ(SOUNDEX(property), SOUNDEX(parameter)). Inherit
                    // the enclosing field's policy, not the parameter's caller-supplied name.
                    capture(function, parameter, Operator.SOUNDS_LIKE, repository);
                }
            }
            for (var child : function.getExpressions()) expression(child, repository);
        }
    }

    private Expression phonetic(Expression expression, PortableSQLRepository<?> repository) {
        if (expression instanceof FunctionApply function
                && ExpressionHelper.hasBuiltinParser(function, repository)
                && function.getOperator() == Operator.SOUNDS_LIKE && function.getExpressions().size() == 1
                && ExpressionHelper.hasBuiltinParser(function.first(), repository)) return function.first();
        return null;
    }

    private void capture(Expression scope, Expression operand, Operator operator,
            PortableSQLRepository<?> repository) {
        if (!(operand instanceof Parameter parameter)
                || !ExpressionHelper.hasBuiltinParser(parameter, repository)
                || parameter.getOperator() != operator) return;
        var policy = ExpressionHelper.parameterPolicy(context, scope, repository);
        if (io.teaql.core.utils.SensitiveLogNames.credential(parameter.getName()))
            policy = SqlParameterLogPolicy.CREDENTIAL;
        output.capture(List.of(policy), new Object[]{parameter.getValue()});
    }

}
