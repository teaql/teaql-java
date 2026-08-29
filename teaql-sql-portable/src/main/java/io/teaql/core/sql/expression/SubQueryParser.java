package io.teaql.core.sql.expression;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.teaql.core.utils.ObjectUtil;

import io.teaql.core.Entity;
import io.teaql.core.Parameter;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SearchRequest;
import io.teaql.core.SmartList;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.internal.TempRequest;
import io.teaql.core.UserContext;
import io.teaql.core.criteria.IN;
import io.teaql.core.criteria.Operator;
import io.teaql.core.sql.portable.PortableSQLRepository;
import io.teaql.core.sql.SQLColumnResolver;

public class SubQueryParser implements SQLExpressionParser<SubQuerySearchCriteria> {

    public static final String IGNORE_SUBTYPES = "IGNORE_SUBTYPES";

    @Override
    public Class<SubQuerySearchCriteria> type() {
        return SubQuerySearchCriteria.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            SubQuerySearchCriteria expression,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        SearchRequest dependsOn = expression.getDependsOn();
        String propertyName = expression.getPropertyName();
        String dependsOnPropertyName = expression.getDependsOnPropertyName();
        String type = dependsOn.getTypeName();

        PortableSQLRepository<?> repository = null;
        if (sqlColumnResolver instanceof PortableSQLRepository) {
            repository = ((PortableSQLRepository<?>) sqlColumnResolver).getResolver().resolve(type);
        }

        if (dependsOn.tryUseSubQuery()
                && repository != null 
                && sqlColumnResolver.canMixinSubQuery(userContext, dependsOn)) {
            PortableSQLRepository<?> subRepository = repository;
            TempRequest tempRequest = new TempRequest(dependsOn.returnType(), dependsOn.getTypeName());

            tempRequest.setOrderBy(dependsOn.getOrderBy());
            tempRequest.setSlice(dependsOn.getSlice());

            // select depends on property
            tempRequest.selectProperty(dependsOnPropertyName);
            tempRequest.appendSearchCriteria(dependsOn.getSearchCriteria());
            // A NULL in an IN projection is irrelevant, but in a NOT(IN(...))
            // relation predicate it poisons every outer comparison. Filtering
            // it for every relation subquery is semantically neutral for IN
            // and keeps orphan foreign keys from breaking HaveNo semantics.
            tempRequest.appendSearchCriteria(
                    tempRequest.createBasicSearchCriteria(
                            dependsOnPropertyName, Operator.IS_NOT_NULL));

            userContext.putAttribute(IGNORE_SUBTYPES, true);
            String subQuery = subRepository.buildDataSQL(userContext, tempRequest, parameters);
            userContext.putAttribute(IGNORE_SUBTYPES, null);

            if (ObjectUtil.isEmpty(subQuery)) {
                return SearchCriteria.FALSE;
            }
            // Inline the pre-compiled subquery SQL directly into the IN clause.
            // RawSql has been removed; we format the IN predicate here.
            String leftColumn = ExpressionHelper.toSql(
                    userContext,
                    new PropertyReference(propertyName),
                    idTable, parameters, sqlColumnResolver);
            return leftColumn + " IN (" + subQuery + ")";
        }

        // fall back
        SmartList<Entity> referred = userContext.internalExecuteForList(dependsOn);
        Set dependsOnValues = new HashSet<>();
        for (Entity entity : referred) {
            Object propertyValue = entity.getProperty(dependsOnPropertyName);
            if (!ObjectUtil.isEmpty(propertyValue)) {
                dependsOnValues.add(propertyValue);
            }
        }
        // The fallback has already materialized the nested result. Bind those
        // identifiers as an ordinary parameterized IN list. IN_LARGE compiles
        // to PostgreSQL's scalar-array syntax and requires one JDBC array; the
        // generic positional adapter expands collections into multiple scalar
        // placeholders, producing invalid "= ANY (?, ...)" SQL.
        Parameter parameter = new Parameter(propertyName, dependsOnValues, Operator.IN);
        IN in = new IN(new PropertyReference(propertyName), parameter);
        return ExpressionHelper.toSql(userContext, in, idTable, parameters, sqlColumnResolver);
    }
}
