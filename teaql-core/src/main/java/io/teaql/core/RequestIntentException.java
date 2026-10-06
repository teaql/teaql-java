package io.teaql.core;

/** Stable, value-free diagnostic for missing request-owned business intent. */
public final class RequestIntentException extends TeaQLRuntimeException {
    private final String code;
    private final String field;
    private final String requestKind;

    RequestIntentException(String code, String field, String requestKind) {
        super("[" + code + "] " + requestKind + " request requires a non-blank " + field);
        this.code = code;
        this.field = field;
        this.requestKind = requestKind;
    }

    public String getCode() { return code; }
    public String getField() { return field; }
    public String getRequestKind() { return requestKind; }

    // Unicode White_Space, matching Rust str::trim rather than Java String.trim/isBlank.
    static boolean blank(String value) {
        return value == null || value.codePoints().allMatch(c ->
                (c >= 0x09 && c <= 0x0d) || c == 0x20 || c == 0x85 || c == 0xa0
                || c == 0x1680 || (c >= 0x2000 && c <= 0x200a) || c == 0x2028
                || c == 0x2029 || c == 0x202f || c == 0x205f || c == 0x3000);
    }

    static String requireComment(String value, String kind) {
        if (blank(value)) throw new RequestIntentException("REQUEST_COMMENT_REQUIRED", "comment", kind);
        return value;
    }
}
