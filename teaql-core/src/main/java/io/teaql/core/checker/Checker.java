package io.teaql.core.checker;

import java.time.LocalDateTime;

import io.teaql.core.utils.NumberUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.UserContext;
import io.teaql.core.checker.internal.CheckerInvocation;

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
        java.util.List list = (java.util.List) CheckerInvocation.attribute(context, TEAQL_DATA_CHECKED_ITEMS);
        if (list == null) {
            list = new java.util.ArrayList();
            CheckerInvocation.attribute(context, TEAQL_DATA_CHECKED_ITEMS, list);
        }
        list.add(entity);
    }

    default boolean needCheck(UserContext context, T entity) {
        if (ObjectUtil.isNull(entity)) {
            return false;
        }

        java.util.List list = (java.util.List) CheckerInvocation.attribute(context, TEAQL_DATA_CHECKED_ITEMS);
        if (list != null) {
            for (Object checked : list) {
                if (checked == entity) return false;
            }
        }

        if (entity.get$status() == EntityStatus.REFER) {
            return false;
        }
        CheckerInvocation invocation = CheckerInvocation.current(context);
        if (invocation != null && invocation.mutationOnly()
                && (entity.get$status() == EntityStatus.PERSISTED
                    || entity.get$status() == EntityStatus.PERSISTED_DELETED)) {
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

    default void invalidTypeCheck(UserContext context, ObjectLocation location, String message) {
        CheckResult result = CheckResult.invalidType(location, message);
        appendResult(context, result);
        result.setNaturalLanguageStatement(message);
    }

    /** Model-derived ownership admission, before any Fix or provider activity. */
    default void verifyContextRoot(UserContext context, T entity, String property,
            String expectedType, ObjectLocation location) {
        var root = context.requireActiveRoot(expectedType);
        Object current = entity.getProperty(property);
        if (!entity.newItem()) {
            var change = entity.__internalPropertyChange(property);
            boolean loaded = change == null ? entity.isPropertyLoaded(property) : change.wasOldValueLoaded();
            Object original = change == null ? current : change.getOldValue();
            Long originalId = contextRootId(context, original, expectedType);
            if (!loaded || originalId == null) {
                invalidTypeCheck(context, location, "Mutation requires a loaded context-bound root");
                return;
            }
            if (!root.id().equals(originalId)) {
                throw new io.teaql.core.ContextRootException(
                        io.teaql.core.ContextRootException.Reason.VALUE_MISMATCH, expectedType, root);
            }
        }
        if (current != null || !entity.newItem()) {
            verifySuppliedContextRoot(context, current, root, expectedType, location);
        }
        // The raw ledger can carry a canonical relation ID independently of
        // the generated getter. Validate it too; never overwrite a conflict.
        if (entity.__internalHasMutationLedger() && entity.getId() != null) {
            var ledger = entity.getEntityMutationLedger();
            var key = new io.teaql.core.EntityKey(entity.typeName(), entity.getId());
            if (ledger.containsField(key, property)) {
                Object supplied = ledger.get(key, property);
                if (supplied != null || !entity.newItem()) {
                    verifySuppliedContextRoot(context, supplied, root, expectedType, location);
                }
            }
        }
    }

    private void verifySuppliedContextRoot(UserContext context, Object value,
            io.teaql.core.ContextEntityRef root, String expectedType, ObjectLocation location) {
        Long id = contextRootId(context, value, expectedType);
        if (id == null) {
            invalidTypeCheck(context, location, "Mutation requires a loaded context-bound root");
        } else if (!root.id().equals(id)) {
            throw new io.teaql.core.ContextRootException(
                    io.teaql.core.ContextRootException.Reason.VALUE_MISMATCH, expectedType, root);
        }
    }

    private static Long contextRootId(UserContext context, Object value, String expectedType) {
        if (value instanceof io.teaql.core.Entity reference) {
            Long id = reference.getId();
            if (id == null || id <= 0) return null;
            if (expectedType.equals(reference.typeName())) return id;
            // References may carry a model-declared subtype. Resolve only from
            // this context; neither a process-global registry nor SQL is valid
            // evidence that a different model type belongs to this root family.
            var metadata = context.capability(io.teaql.core.meta.EntityMetaFactory.class);
            if (metadata == null) return null;
            io.teaql.core.meta.EntityDescriptor declared, actual;
            try {
                declared = metadata.resolveEntityDescriptor(expectedType);
                actual = metadata.resolveEntityDescriptor(reference.typeName());
            } catch (io.teaql.core.TeaQLRuntimeException unknownType) {
                return null;
            }
            if (declared == null || actual == null
                    || declared.getTargetType() == null || actual.getTargetType() == null
                    || !declared.getTargetType().isInstance(reference)
                    || !actual.getTargetType().isInstance(reference)) return null;
            var visited = java.util.Collections.newSetFromMap(
                    new java.util.IdentityHashMap<io.teaql.core.meta.EntityDescriptor, Boolean>());
            for (var descriptor = actual; descriptor != null && visited.add(descriptor);
                    descriptor = descriptor.getParent()) {
                if (descriptor == declared) return id;
            }
            return null;
        }
        return value instanceof Long id && id > 0 ? id : null;
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
        // Rule factories do not know the declaring entity. Bind that identity
        // before localization/wire rendering, without replacing a more precise
        // identity already supplied by a custom or specialized checker.
        if (result.getRootType() == null || result.getRootType().isBlank()) {
            result.setRootType(type());
        }
        context.translateCheckResult(result);
        java.util.List list = (java.util.List) CheckerInvocation.attribute(context, TEAQL_DATA_CHECK_RESULT);
        if (list == null) {
            list = new java.util.ArrayList();
            CheckerInvocation.attribute(context, TEAQL_DATA_CHECK_RESULT, list);
        }
        list.add(result);
    }

    /**
     * Standalone entry point. Recursive checks inside an existing graph/check
     * share that invocation; a top-level check owns and releases its clock,
     * traversal guard and detached evidence receipt just like graph save.
     * A standalone owner throws collected violations before releasing them;
     * joined callbacks leave that decision to the existing graph/check owner.
     * The three-argument method is the implementation/traversal callback.
     */
    @SuppressWarnings("unchecked")
    default void checkAndFix(UserContext context, T entity) {
        if (CheckerInvocation.current(context) != null) {
            checkAndFix(context, entity, null);
            return;
        }
        try (var invocation = CheckerInvocation.open(context)) {
            checkAndFix(context, entity, null);
            java.util.List<CheckResult> violations = (java.util.List<CheckResult>)
                    invocation.attribute(TEAQL_DATA_CHECK_RESULT);
            if (violations != null && !violations.isEmpty()) {
                throw new CheckException(new java.util.ArrayList<>(violations));
            }
        }
    }
}
