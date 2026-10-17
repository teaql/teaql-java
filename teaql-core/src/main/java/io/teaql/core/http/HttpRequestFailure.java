package io.teaql.core.http;

/** A stable, safe HTTP failure category. Provider messages and request data are never retained. */
public final class HttpRequestFailure extends RuntimeException {
    public enum Code {
        INVALID_INPUT, PROFILE_NOT_CONFIGURED, TIMEOUT, CANCELLED, RATE_LIMITED,
        CIRCUIT_OPEN, RESPONSE_TOO_LARGE, MALFORMED_RESPONSE, UPSTREAM_FAILURE, AMBIGUOUS_OUTCOME
    }

    private final Code code;

    public HttpRequestFailure(Code code) {
        super("HTTP request failed: " + code.name());
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
