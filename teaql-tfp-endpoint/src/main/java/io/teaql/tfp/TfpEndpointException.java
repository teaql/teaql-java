package io.teaql.tfp;

public class TfpEndpointException extends RuntimeException {
    private final String code;

    public TfpEndpointException(String code, String message) {
        super(message);
        this.code = code;
    }

    public TfpEndpointException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() { return code; }
}
