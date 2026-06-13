package io.teaql.core;

public class ConcurrentModifyException extends TeaQLRuntimeException {
    public ConcurrentModifyException() {
    }

    public ConcurrentModifyException(String message) {
        super(message);
    }

    public ConcurrentModifyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConcurrentModifyException(Throwable cause) {
        super(cause);
    }

    public ConcurrentModifyException(
            String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
