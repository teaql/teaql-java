package io.teaql.core.memory.filter;

import java.util.List;

import io.teaql.core.utils.ArrayUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.BaseEntity;
import io.teaql.core.Entity;
import io.teaql.core.Expression;
import io.teaql.core.Parameter;
import io.teaql.core.PropertyFunction;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.TQLException;
import io.teaql.core.criteria.AND;
import io.teaql.core.criteria.Between;
import io.teaql.core.criteria.NOT;
import io.teaql.core.criteria.OR;
import io.teaql.core.criteria.OneOperatorCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import io.teaql.core.criteria.VersionSearchCriteria;

public class CriteriaFilter<T extends Entity> implements Filter<T> {
    @Override
    public boolean accept(T entity, SearchCriteria searchCriteria) {
        if (entity == null) {
            return false;
        }

        if (searchCriteria == null) {
            return true;
        }

        if (searchCriteria instanceof AND and) {
            return and(entity, and);
        }

        if (searchCriteria instanceof OR or) {
            return or(entity, or);
        }

        if (searchCriteria instanceof NOT not) {
            return not(entity, not);
        }

        if (searchCriteria instanceof OneOperatorCriteria oneOperatorCriteria) {
            return oneOperatorCriteria(entity, oneOperatorCriteria);
        }

        if (searchCriteria instanceof VersionSearchCriteria versionSearchCriteria) {
            return accept(entity, versionSearchCriteria.getSearchCriteria());
        }

        if (searchCriteria instanceof TwoOperatorCriteria twoOperatorsCriteria) {
            return twoOperatorCriteria(entity, twoOperatorsCriteria);
        }

        if (searchCriteria instanceof Between between) {
            return between(entity, between);
        }

        throw new TQLException("unsupported searchCriteria:" + searchCriteria);
    }

    public boolean between(T entity, Between between) {
        Expression first = between.first();
        Expression second = between.second();
        Expression third = between.third();
        if (!(first instanceof PropertyReference)) {
            throw new TQLException("unsupported Between, first element is not PropertyReference");
        }

        if (!(second instanceof Parameter)) {
            throw new TQLException("unsupported Between, second element is not Parameter");
        }

        if (!(third instanceof Parameter)) {
            throw new TQLException("unsupported Between, third element is not Parameter");
        }

        String propertyName = ((PropertyReference) first).getPropertyName();
        Object start = ((Parameter) second).getValue();
        Object end = ((Parameter) third).getValue();
        Object propertyValue = entity.getProperty(propertyName);
        int compare = ObjectUtil.compare((Comparable) propertyValue, (Comparable) start);
        if (compare < 0) {
            return false;
        }
        compare = ObjectUtil.compare((Comparable) propertyValue, (Comparable) end);
        if (compare > 0) {
            return false;
        }
        return true;
    }

    public boolean twoOperatorCriteria(T entity, TwoOperatorCriteria twoOperatorsCriteria) {
        PropertyFunction operator = twoOperatorsCriteria.getOperator();
        Expression first = twoOperatorsCriteria.first();
        Expression second = twoOperatorsCriteria.second();
        if (!(first instanceof PropertyReference)) {
            throw new TQLException(
                    "unsupported twoOperatorCriteria, first element is not PropertyReference");
        }
        if (!(second instanceof Parameter)) {
            throw new TQLException("unsupported twoOperatorCriteria, second element is not Parameter");
        }

        String propertyName = ((PropertyReference) first).getPropertyName();
        Object value = ((Parameter) second).getValue();
        Object propertyValue = entity.getProperty(propertyName);
        if (propertyValue instanceof BaseEntity) {
            propertyValue = ((BaseEntity) propertyValue).getId();
        }

        int compare = 0;
        if (!ArrayUtil.isArray(value)) {
            compare = ObjectUtil.compare((Comparable) propertyValue, (Comparable) value);
        }
        switch (((Operator) operator)) {
            case EQUAL -> {
                return compare == 0;
            }
            case NOT_EQUAL -> {
                return compare != 0;
            }
            case LESS_THAN -> {
                return compare < 0;
            }
            case LESS_THAN_OR_EQUAL -> {
                return compare <= 0;
            }
            case GREATER_THAN -> {
                return compare > 0;
            }
            case GREATER_THAN_OR_EQUAL -> {
                return compare >= 0;
            }
            case CONTAIN -> {
                return StrUtil.contains((String) propertyValue, (String) value);
            }
            case NOT_CONTAIN -> {
                return !StrUtil.contains((String) propertyValue, (String) value);
            }
            case BEGIN_WITH -> {
                return StrUtil.startWith((String) propertyValue, (String) value);
            }
            case NOT_BEGIN_WITH -> {
                return !StrUtil.startWith((String) propertyValue, (String) value);
            }
            case END_WITH -> {
                return StrUtil.endWith((String) propertyValue, (String) value);
            }
            case NOT_END_WITH -> {
                return !StrUtil.endWith((String) propertyValue, (String) value);
            }
            case IN, IN_LARGE -> {
                if (ArrayUtil.isArray(value)) {
                    int length = ArrayUtil.length(value);
                    for (int i = 0; i < length; i++) {
                        Object o = ArrayUtil.get(value, i);
                        compare = ObjectUtil.compare((Comparable) propertyValue, (Comparable) value);
                        if (compare == 0) {
                            return true;
                        }
                    }
                    return false;
                }
                throw new TQLException("operator  in/in large should have the array parameter");
            }
            case NOT_IN, NOT_IN_LARGE -> {
                if (ArrayUtil.isArray(value)) {
                    int length = ArrayUtil.length(value);
                    for (int i = 0; i < length; i++) {
                        Object o = ArrayUtil.get(value, i);
                        compare = ObjectUtil.compare((Comparable) propertyValue, (Comparable) value);
                        if (compare == 0) {
                            return false;
                        }
                    }
                    return true;
                }
                throw new TQLException("operator not in/not in large should have the array parameter");
            }
        }
        return false;
    }

    public boolean oneOperatorCriteria(T entity, OneOperatorCriteria oneOperatorCriteria) {
        PropertyFunction operator = oneOperatorCriteria.getOperator();
        Expression firstExpression = oneOperatorCriteria.first();
        if (firstExpression instanceof PropertyReference) {
            throw new TQLException("one expression is expected in oneOperatorCriteria:" + operator);
        }
        PropertyReference prop = (PropertyReference) firstExpression;
        String propertyName = prop.getPropertyName();
        if (operator == Operator.IS_NOT_NULL) {
            return ObjectUtil.isNotNull(entity.getProperty(propertyName));
        }
        else if (operator == Operator.IS_NULL) {
            return ObjectUtil.isNull(entity.getProperty(propertyName));
        }
        throw new TQLException("unsupported operator in oneOperatorCriteria:" + operator);
    }

    public boolean not(T entity, NOT not) {
        List<Expression> expressions = not.getExpressions();
        for (Expression expression : expressions) {
            if (expression instanceof SearchCriteria sub) {
                return !accept(entity, sub);
            }
            else {
                throw new TQLException("unexpected search criteria in or:" + expression);
            }
        }
        return false;
    }

    public boolean or(T entity, OR or) {
        List<Expression> expressions = or.getExpressions();
        for (Expression expression : expressions) {
            if (expression instanceof SearchCriteria sub) {
                boolean accept = accept(entity, sub);
                if (accept) {
                    return true;
                }
            }
            else {
                throw new TQLException("unexpected search criteria in or:" + expression);
            }
        }
        return false;
    }

    public boolean and(T entity, AND and) {
        List<Expression> expressions = and.getExpressions();
        for (Expression expression : expressions) {
            if (expression instanceof SearchCriteria sub) {
                boolean accept = accept(entity, sub);
                if (!accept) {
                    return false;
                }
            }
            else {
                throw new TQLException("unexpected search criteria in and:" + expression);
            }
        }
        return true;
    }
}
