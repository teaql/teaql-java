package io.teaql.core.secret;

/** Deliberately has no provider cause, file path, variable name or secret text. */
public final class SecretFailure extends RuntimeException {
    public enum Code {
        INVALID_INPUT, PROVIDER_NOT_CONFIGURED, UNKNOWN_BINDING, MISSING,
        DENIED, EXPIRED, UNAVAILABLE
    }

    private final Code code;

    public SecretFailure(Code code) {
        super("secret " + code.name().toLowerCase(java.util.Locale.ROOT));
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
