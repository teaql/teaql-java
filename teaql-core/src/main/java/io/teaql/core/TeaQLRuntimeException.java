package io.teaql.core;

public class TeaQLRuntimeException extends RuntimeException {
    public TeaQLRuntimeException() {
    }

    public TeaQLRuntimeException(String message) {
        super(message);
    }

    public TeaQLRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public TeaQLRuntimeException(Throwable cause) {
        super(cause);
    }

    public TeaQLRuntimeException(
            String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
