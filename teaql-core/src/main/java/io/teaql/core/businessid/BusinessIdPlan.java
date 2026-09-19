package io.teaql.core.businessid;

import java.time.LocalDate;

public record BusinessIdPlan(
        BusinessIdDefinition definition,
        BusinessIdScope scope,
        LocalDate businessDate,
        String dateText,
        long maximumSequence) {
}
