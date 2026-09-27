package io.teaql.runtime.businessid;

import io.teaql.core.businessid.BusinessIdDefinition;
import io.teaql.core.businessid.BusinessIdErrorCode;
import io.teaql.core.businessid.BusinessIdException;
import io.teaql.core.businessid.BusinessIdPlan;
import io.teaql.core.businessid.BusinessIdScope;
import java.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

public class InMemoryBusinessIdAllocatorTest {
    @Test
    public void invalidInitialSequenceRangeFailsBeforeAllocation() {
        BusinessIdDefinition definition = BusinessIdDefinition.dailySequence(
                "order_number", "CO", "commerce_order");
        LocalDate date = LocalDate.of(2026, 9, 23);
        BusinessIdScope scope = new BusinessIdScope(
                "domain-root-a", "commerce_order", "commerce_order", "20260923");

        BusinessIdException invalid = Assert.assertThrows(
                BusinessIdException.class,
                () -> new BusinessIdPlan(definition, scope, date, "20260923", 2, 1));
        Assert.assertEquals(
                BusinessIdErrorCode.BUSINESS_ID_DEFINITION_INVALID, invalid.getCode());
    }

    @Test
    public void exhaustedAllocationDoesNotConsumeTheNextSequence() {
        InMemoryBusinessIdAllocator allocator = new InMemoryBusinessIdAllocator();
        BusinessIdDefinition definition = BusinessIdDefinition.dailySequence(
                "order_number", "CO", "commerce_order");
        LocalDate date = LocalDate.of(2026, 9, 23);
        BusinessIdScope scope = new BusinessIdScope(
                "tenant-a", "commerce_order", "commerce_order", "20260923");
        BusinessIdPlan limited = new BusinessIdPlan(
                definition, scope, date, "20260923", 0, 0);

        Assert.assertEquals(0, allocator.allocate(limited).sequence());
        for (int attempt = 0; attempt < 2; attempt++) {
            BusinessIdException exhausted = Assert.assertThrows(
                    BusinessIdException.class, () -> allocator.allocate(limited));
            Assert.assertEquals(
                    BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED,
                    exhausted.getCode());
        }

        // A later policy may increase the range; rejected attempts must not make a gap.
        BusinessIdPlan expanded = new BusinessIdPlan(
                definition, scope, date, "20260923", 0, 1);
        Assert.assertEquals(1, allocator.allocate(expanded).sequence());
    }
}
