package io.teaql.core;

public class TeaQLCheckedException extends Exception {
    public TeaQLCheckedException() {
    }

    public TeaQLCheckedException(String message) {
        super(message);
    }

    public TeaQLCheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TeaQLCheckedException(Throwable cause) {
        super(cause);
    }

    public TeaQLCheckedException(
            String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
