package io.teaql.cloud;

public class CloudException extends RuntimeException {
    public CloudException(String message) { super(message); }
    public CloudException(String message, Throwable cause) { super(message, cause); }
}
