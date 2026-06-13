package io.teaql.autoconfigure.log;

import io.teaql.core.UserContext;
import io.teaql.core.DefaultUserContext;
import io.teaql.core.UserContextInitializer;

import java.util.List;

import org.slf4j.MDC;
import org.springframework.core.PriorityOrdered;

import io.teaql.core.utils.IdUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.StrUtil;

public class UserTraceIdInitializer implements UserContextInitializer, PriorityOrdered {

    public static final String TRACE_ID = "TRACE_ID";
    public static final String TRACE_PREFIX = "TRACE_";
    public static final String TRACE_PATH = "TRACE_PATH";

    @Override
    public boolean support(Object request) {
        return true;
    }

    @Override
    public void init(UserContext userContext, Object request) {
        if (!(userContext instanceof DefaultUserContext)) {
            return;
        }
        DefaultUserContext dctx = (DefaultUserContext) userContext;
        List<String> headerNames = dctx.getHeaderNames();
        for (String headerName : headerNames) {
            if (StrUtil.startWithIgnoreCase(headerName, TRACE_PREFIX)) {
                MDC.put(headerName.toUpperCase(), dctx.getHeader(headerName));
            }
        }
        List<String> parameterNames = dctx.getParameterNames();
        for (String parameterName : parameterNames) {
            if (StrUtil.startWithIgnoreCase(parameterName, TRACE_PREFIX)) {
                MDC.put(parameterName.toUpperCase(), dctx.getParameter(parameterName));
            }
        }
        String traceId = MDC.get(TRACE_ID);
        if (ObjectUtil.isEmpty(traceId)) {
            traceId = IdUtil.getSnowflakeNextIdStr();
            MDC.put(TRACE_ID, 'T' + traceId);
        }
        MDC.put(TRACE_PATH, dctx.requestUri());
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 1;
    }
}
