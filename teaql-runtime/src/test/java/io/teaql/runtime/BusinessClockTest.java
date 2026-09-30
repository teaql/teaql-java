package io.teaql.runtime;

import static org.junit.Assert.assertEquals;

import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.Test;

public class BusinessClockTest {

    @Test
    public void contextUsesTheRuntimeClockForDateTimeAndNowExpression() {
        LocalDateTime expected = LocalDateTime.of(2032, 2, 29, 10, 15, 30);
        BusinessClock fixedClock = new BusinessClock() {
            @Override
            public LocalDate businessDate(UserContext context) {
                return expected.toLocalDate();
            }

            @Override
            public LocalDateTime businessDateTime(UserContext context) {
                return expected;
            }
        };
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory())
                .businessClock(fixedClock)
                .build();

        DefaultUserContext context = new DefaultUserContext(runtime);

        assertEquals(expected.toLocalDate(), context.businessDate());
        assertEquals(expected, context.businessTime());
        assertEquals(expected, context.<LocalDateTime>evaluate("now"));
    }

    @Test
    public void existingDateOnlyClockRemainsSourceCompatibleAndDeterministic() {
        LocalDate expected = LocalDate.of(2033, 1, 2);
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory())
                .businessClock(ignored -> expected)
                .build();

        DefaultUserContext context = new DefaultUserContext(runtime);

        assertEquals(expected.atStartOfDay(), context.businessTime());
    }
}
