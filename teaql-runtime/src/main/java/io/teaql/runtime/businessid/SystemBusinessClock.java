package io.teaql.runtime.businessid;

import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessClock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Host time is isolated behind a context capability and can be replaced in tests. */
public final class SystemBusinessClock implements BusinessClock {
    public static final SystemBusinessClock INSTANCE = new SystemBusinessClock();

    private SystemBusinessClock() {
    }

    @Override
    public LocalDate businessDate(UserContext context) {
        return businessDateTime(context).toLocalDate();
    }

    @Override
    public LocalDateTime businessDateTime(UserContext context) {
        return LocalDateTime.now(resolveZone(context));
    }

    private ZoneId resolveZone(UserContext context) {
        Object zone = context.getAttribute("teaql.business.zone");
        return zone instanceof ZoneId ? (ZoneId) zone : ZoneId.systemDefault();
    }
}
