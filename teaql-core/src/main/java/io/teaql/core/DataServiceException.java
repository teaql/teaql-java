package io.teaql.core;

public class DataServiceException extends TeaQLRuntimeException {
    public DataServiceException(String message) {
        super(message);
    }

    public DataServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
