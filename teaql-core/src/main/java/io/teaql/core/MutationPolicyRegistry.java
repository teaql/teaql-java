package io.teaql.core;

import java.util.Optional;

@FunctionalInterface
public interface MutationPolicyRegistry {
    Optional<MutationPolicy> resolve(String requestKey);

    static MutationPolicyRegistry empty() { return requestKey -> Optional.empty(); }
}
