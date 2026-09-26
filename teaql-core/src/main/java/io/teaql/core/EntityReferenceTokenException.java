package io.teaql.core;

/** Stable fail-closed error; deliberately does not reveal why validation failed. */
public final class EntityReferenceTokenException extends TeaQLRuntimeException {
    private final String code;

    public EntityReferenceTokenException(String code) {
        super(code);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
