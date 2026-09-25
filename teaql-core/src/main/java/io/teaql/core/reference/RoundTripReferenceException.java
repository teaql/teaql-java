package io.teaql.core.reference;

public class RoundTripReferenceException extends RuntimeException {
    private final RoundTripReferenceErrorCode code;

    public RoundTripReferenceException(RoundTripReferenceErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public RoundTripReferenceException(
            RoundTripReferenceErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public RoundTripReferenceErrorCode getCode() {
        return code;
    }
}
