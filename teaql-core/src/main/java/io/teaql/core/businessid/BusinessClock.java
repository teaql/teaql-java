package io.teaql.core.businessid;

import io.teaql.core.UserContext;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Supplies domain-visible time through the current operation context. */
public interface BusinessClock {
    LocalDate businessDate(UserContext context);

    /**
     * Returns the complete business date-time. Existing date-only clock
     * implementations remain deterministic and resolve to start of day.
     */
    default LocalDateTime businessDateTime(UserContext context) {
        return businessDate(context).atStartOfDay();
    }
}
