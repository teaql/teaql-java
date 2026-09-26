package io.teaql.runtime.reference;

public final class RoundTripReferenceConfiguration {
    public static final String RAW_ID_ENV = "TEAQL_UNSAFE_EXPOSE_RAW_ENTITY_IDS";
    public static final String RAW_ID_ACKNOWLEDGEMENT =
            "I_UNDERSTAND_THIS_EXPOSES_INTERNAL_ENTITY_IDS_FOR_LOCAL_DEBUGGING_ONLY";

    private RoundTripReferenceConfiguration() {}

}
