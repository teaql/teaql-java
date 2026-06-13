package io.teaql.core;

public class EntityActionException extends RuntimeException {
    public EntityActionException() {
        super();
    }

    public EntityActionException(String message) {
        super(message);
    }

    public EntityActionException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityActionException(Throwable cause) {
        super(cause);
    }

    protected EntityActionException(
            String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
