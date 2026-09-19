package io.teaql.core.businessid;

import io.teaql.core.UserContext;
import java.time.LocalDate;

public interface BusinessClock {
    LocalDate businessDate(UserContext context);
}
