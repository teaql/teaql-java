package io.teaql.core.businessid;

import java.time.LocalDate;

public record BusinessIdGenerationRequest(
        BusinessIdDefinition definition,
        String domainRootKey,
        String aggregateType,
        LocalDate businessDate) {
}
