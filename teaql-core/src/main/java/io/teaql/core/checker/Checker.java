package io.teaql.core.checker;

import java.time.LocalDateTime;

import io.teaql.core.utils.NumberUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.UserContext;

/**
 * check or set (default) values for the entity before persist
 */
public interface Checker<T extends BaseEntity> {

    String TEAQL_DATA_CHECK_RESULT = "teaql_data_check_result";
    String TEAQL_DATA_CHECKED_ITEMS = "teaql_data_checkedItems";
    String TEAQL_FIX_TIME = "teaql_fix_time";

    String type();

    void checkAndFix(UserContext context, T entity, ObjectLocation location);

    default void markAsChecked(UserContext context, T entity) {
        java.util.List list = (java.util.List) context.getAttribute(TEAQL_DATA_CHECKED_ITEMS);
        if (list == null) {
            list = new java.util.ArrayList();
            context.putAttribute(TEAQL_DATA_CHECKED_ITEMS, list);
        }
        list.add(entity);
    }

    default boolean needCheck(UserContext context, T entity) {
        if (ObjectUtil.isNull(entity)) {
            return false;
        }

        java.util.List list = (java.util.List) context.getAttribute(TEAQL_DATA_CHECKED_ITEMS);
        if (list != null && list.contains(entity)) {
            return false;
        }

        if (entity.get$status() == EntityStatus.REFER) {
            return false;
        }
        return true;
    }

    default ObjectLocation newLocation(ObjectLocation parent, String member) {
        if (ObjectUtil.isEmpty(parent)) {
            return ObjectLocation.hashRoot(member);
        }
        return parent.member(member);
    }

    default ObjectLocation newLocation(ObjectLocation parent, String member, int index) {
        return newLocation(parent, member).element(index);
    }

    default void requiredCheck(UserContext context, ObjectLocation location, Object current) {
        if (ObjectUtil.isNull(current)) {
            appendResult(context, CheckResult.required(location));
        }
    }

    default void minNumberCheck(
            UserContext context, ObjectLocation location, Number minNumber, Number current) {
        if (NumberUtil.isLess(NumberUtil.toBigDecimal(current), NumberUtil.toBigDecimal(minNumber))) {
            appendResult(context, CheckResult.min(location, minNumber, current));
        }
    }

    default void maxNumberCheck(
            UserContext context, ObjectLocation location, Number maxNumber, Number current) {
        if (NumberUtil.isGreater(
                NumberUtil.toBigDecimal(current), NumberUtil.toBigDecimal(maxNumber))) {
            appendResult(context, CheckResult.max(location, maxNumber, current));
        }
    }

    default void minStringCheck(
            UserContext context, ObjectLocation location, int minLen, CharSequence value) {
        if (StrUtil.length(value) < minLen) {
            appendResult(context, CheckResult.minStr(location, minLen, value));
        }
    }

    default void maxStringCheck(
            UserContext context, ObjectLocation location, int maxLen, CharSequence value) {
        if (StrUtil.length(value) > maxLen) {
            appendResult(context, CheckResult.maxStr(location, maxLen, value));
        }
    }

    default void minDateTimeCheck(
            UserContext context, ObjectLocation location, LocalDateTime minDate, LocalDateTime value) {
        if (value.isBefore(minDate)) {
            appendResult(context, CheckResult.minDate(location, minDate, value));
        }
    }

    default void maxDateTimeCheck(
            UserContext context, ObjectLocation location, LocalDateTime maxDate, LocalDateTime value) {
        if (value.isAfter(maxDate)) {
            appendResult(context, CheckResult.maxDate(location, maxDate, value));
        }
    }

    default void appendResult(UserContext context, CheckResult result) {
        context.translateCheckResult(result);
        java.util.List list = (java.util.List) context.getAttribute(TEAQL_DATA_CHECK_RESULT);
        if (list == null) {
            list = new java.util.ArrayList();
            context.putAttribute(TEAQL_DATA_CHECK_RESULT, list);
        }
        list.add(result);
    }

    default void checkAndFix(UserContext context, T entity) {
        checkAndFix(context, entity, null);
    }
}
