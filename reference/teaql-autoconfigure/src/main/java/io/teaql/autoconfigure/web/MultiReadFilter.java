package io.teaql.autoconfigure.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import io.teaql.core.utils.StrUtil;

import static io.teaql.autoconfigure.web.ServletUserContextInitializer.USER_CONTEXT;

import io.teaql.core.UserContext;
import io.teaql.core.log.Markers;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MultiReadFilter implements OrderedFilter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof CachedBodyHttpServletRequest)) {
            request = new CachedBodyHttpServletRequest((HttpServletRequest) request);
        }
        if (!(response instanceof ContentCachingResponseWrapper)) {
            response = new ContentCachingResponseWrapper((HttpServletResponse) response);
        }
        chain.doFilter(request, response);
        UserContext userContext = (UserContext) request.getAttribute(USER_CONTEXT);
        if (userContext != null) {
            ContentCachingResponseWrapper responseWrapper = (ContentCachingResponseWrapper) response;
            Collection<String> headerNames = responseWrapper.getHeaderNames();
            for (String headerName : headerNames) {
                userContext.debug(
                        Markers.HTTP_RESPONSE,
                        "HEADER {}={}",
                        headerName,
                        responseWrapper.getHeader(headerName));
            }
            String responseBody = getResponseBody(response);
            if (StrUtil.length(responseBody) < 1000) {
                userContext.debug(Markers.HTTP_SHORT_RESPONSE, "Response body: {}", responseBody);
            }
            else {
                userContext.debug(Markers.HTTP_RESPONSE, "Response body: {}", responseBody);
            }
        }
        ((ContentCachingResponseWrapper) response).copyBodyToResponse();
    }

    private String getResponseBody(ServletResponse response) {
        ContentCachingResponseWrapper wrapper =
                WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                String payload;
                try {
                    payload = new String(buf, "UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    payload = "[unknown]";
                }
                return payload;
            }
        }
        return "";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
