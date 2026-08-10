package io.teaql.core.spi;

public class CacheException extends RuntimeException {
    public CacheException(String message) {
        super(message);
    }
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
