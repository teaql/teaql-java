package io.teaql.autoconfigure.log;

import java.util.List;

import org.springframework.core.Ordered;

import io.teaql.core.UserContext;
import io.teaql.core.DefaultUserContext;
import io.teaql.core.log.Markers;
import io.teaql.core.UserContextInitializer;

public class RequestLogger implements UserContextInitializer, Ordered {

    @Override
    public boolean support(Object request) {
        return true;
    }

    @Override
    public void init(UserContext userContext, Object request) {
        if (!(userContext instanceof DefaultUserContext dctx)) {
            return;
        }
        userContext.debug(
                Markers.HTTP_SHORT_REQUEST, "{} {}", dctx.method(), dctx.requestUri());
        List<String> headerNames = dctx.getHeaderNames();
        for (String headerName : headerNames) {
            userContext.debug(
                    Markers.HTTP_REQUEST, "HEADER {}={}", headerName, dctx.getHeader(headerName));
        }

        List<String> parameterNames = dctx.getParameterNames();
        for (String parameterName : parameterNames) {
            userContext.debug(
                    Markers.HTTP_SHORT_REQUEST,
                    "PARAM {}={}",
                    parameterName,
                    dctx.getParameter(parameterName));
        }

        byte[] bodyBytes = dctx.getBodyBytes();
        if (bodyBytes != null) {
            String body = new String(bodyBytes);
            if (body.length() < 1000) {
                userContext.debug(Markers.HTTP_SHORT_REQUEST, "BODY: {}", body);
            }
            else {
                userContext.debug(Markers.HTTP_REQUEST, "BODY: {}", body);
            }
        }
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}
