package com.teaql.example;

import io.teaql.core.MutationDecision;
import io.teaql.core.MutationOperationKind;
import io.teaql.core.MutationPlan;
import io.teaql.core.MutationPolicy;
import io.teaql.core.MutationPolicyIdentity;
import io.teaql.core.UserContext;
import java.math.BigDecimal;
import java.util.List;

/** Customer-owned whole-graph policy for the order-management example. */
public final class OrderMutationPolicy implements MutationPolicy {
    public static final String HIGH_VALUE_PERMISSION = "order.submitHighValue";
    public static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000.00");
    public static final MutationPolicyIdentity IDENTITY = new MutationPolicyIdentity(
            "example.order-mutation", "1.0.0", "example:order-policy-2026-09-29");

    @Override
    public MutationPolicyIdentity identity() {
        return IDENTITY;
    }

    @Override
    public MutationDecision review(UserContext context, MutationPlan plan) {
        boolean containsHighValueOrder = plan.operations().stream()
                .filter(operation -> operation.kind() == MutationOperationKind.CREATE
                        || operation.kind() == MutationOperationKind.UPDATE)
                .map(operation -> operation.changedValues().get("totalAmount"))
                .filter(BigDecimal.class::isInstance)
                .map(BigDecimal.class::cast)
                .anyMatch(amount -> amount.compareTo(HIGH_VALUE_THRESHOLD) > 0);

        if (!containsHighValueOrder) {
            return MutationDecision.allow();
        }

        MutationAuthority authority = context.capability(MutationAuthority.class);
        if (authority != null && authority.permits(HIGH_VALUE_PERMISSION)) {
            return MutationDecision.allow();
        }

        return MutationDecision.deny(
                "ORDER-HIGH-VALUE-AUTHORITY-REQUIRED",
                "Orders above 10000.00 require the high-value order authority",
                List.of("totalAmount"));
    }
}
