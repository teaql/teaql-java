package io.teaql.core;

public interface MutationPolicy {
    MutationPolicyIdentity identity();
    MutationDecision review(UserContext context, MutationPlan plan);
}
