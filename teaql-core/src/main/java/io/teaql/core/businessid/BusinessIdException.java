package io.teaql.core.businessid;

public class BusinessIdException extends RuntimeException {
    private final BusinessIdErrorCode code;

    public BusinessIdException(BusinessIdErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessIdException(BusinessIdErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BusinessIdErrorCode getCode() {
        return code;
    }
}
