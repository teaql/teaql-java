package io.teaql.core.sql.expression;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.teaql.core.utils.ObjectUtil;

import io.teaql.core.Entity;
import io.teaql.core.Parameter;
import io.teaql.core.PropertyReference;
import io.teaql.core.Repository;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SearchRequest;
import io.teaql.core.SmartList;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.internal.TempRequest;
import io.teaql.core.UserContext;
import io.teaql.core.DefaultUserContext;
import io.teaql.core.criteria.IN;
import io.teaql.core.criteria.InLarge;
import io.teaql.core.criteria.Operator;
import io.teaql.core.sql.SQLRepository;
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
        Repository repository = userContext.resolveRepository(type);

        if (dependsOn.tryUseSubQuery()
                && hasSameDatasource(userContext, sqlColumnResolver, repository) && sqlColumnResolver.canMixinSubQuery(userContext, dependsOn)) {
            SQLRepository subRepository = (SQLRepository) repository;
            TempRequest tempRequest = new TempRequest(dependsOn.returnType(), dependsOn.getTypeName());

            tempRequest.setOrderBy(dependsOn.getOrderBy());

            tempRequest.setSlice(dependsOn.getSlice());

            // select depends on property
            tempRequest.selectProperty(dependsOnPropertyName);
            tempRequest.appendSearchCriteria(dependsOn.getSearchCriteria());

            userContext.put(IGNORE_SUBTYPES, true);
            String subQuery = subRepository.buildDataSQL(userContext, tempRequest, parameters);
            if (userContext instanceof DefaultUserContext) {
                ((DefaultUserContext) userContext).del(IGNORE_SUBTYPES);
            }
            if (ObjectUtil.isEmpty(subQuery)) {
                return SearchCriteria.FALSE;
            }
            IN in = new IN(new PropertyReference(propertyName), new RawSql(subQuery));
            return ExpressionHelper.toSql(userContext, in, idTable, parameters, sqlColumnResolver);
        }

        // fall back
        SmartList<Entity> referred = repository.executeForList(userContext, dependsOn);
        Set dependsOnValues = new HashSet<>();
        for (Entity entity : referred) {
            Object propertyValue = entity.getProperty(dependsOnPropertyName);
            if (!ObjectUtil.isEmpty(propertyValue)) {
                dependsOnValues.add(propertyValue);
            }
        }
        Parameter parameter = new Parameter(propertyName, dependsOnValues, Operator.IN_LARGE);
        InLarge in = new InLarge(new PropertyReference(propertyName), parameter);
        return ExpressionHelper.toSql(userContext, in, idTable, parameters, sqlColumnResolver);
    }

    private boolean hasSameDatasource(
            UserContext pUserContext, SQLColumnResolver pSqlColumnResolver, Repository pRepository) {
        if (!(pSqlColumnResolver instanceof SQLRepository)) {
            return false;
        }
        return ((SQLRepository<?>) pSqlColumnResolver).hasSameDataSource(pUserContext, pRepository);
    }
}
