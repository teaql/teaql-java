package io.teaql.runtime.businessid;

import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessClock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Host time is isolated behind a context capability and can be replaced in tests. */
public final class SystemBusinessClock implements BusinessClock {
    public static final SystemBusinessClock INSTANCE = new SystemBusinessClock();

    private SystemBusinessClock() {
    }

    @Override
    public LocalDate businessDate(UserContext context) {
        Object zone = context.getAttribute("teaql.business.zone");
        ZoneId zoneId = zone instanceof ZoneId ? (ZoneId) zone : ZoneId.systemDefault();
        return LocalDate.now(zoneId);
    }
}
