package io.teaql.runtime.reference;

import io.teaql.core.reference.RoundTripReferenceProvider;

public final class RoundTripReferenceConfiguration {
    public static final String RAW_ID_ENV = "TEAQL_UNSAFE_EXPOSE_RAW_ENTITY_IDS";
    public static final String RAW_ID_ACKNOWLEDGEMENT =
            "I_UNDERSTAND_THIS_EXPOSES_INTERNAL_ENTITY_IDS_FOR_LOCAL_DEBUGGING_ONLY";

    private RoundTripReferenceConfiguration() {}

    public enum Mode { GOVERNED, RAW_DIAGNOSTIC }

    public static Mode mode(String deploymentProfile, String acknowledgement) {
        if (!RAW_ID_ACKNOWLEDGEMENT.equals(acknowledgement)) return Mode.GOVERNED;
        if (!"development".equals(deploymentProfile) && !"test".equals(deploymentProfile)) {
            throw new IllegalStateException(
                    RAW_ID_ENV + " cannot enable raw entity IDs outside development/test");
        }
        return Mode.RAW_DIAGNOSTIC;
    }

    /** Selects once at runtime startup; Web adapters only consume the resulting provider. */
    public static RoundTripReferenceProvider configuredProvider(
            String deploymentProfile, RoundTripReferenceProvider governedProvider) {
        return configuredProvider(
                deploymentProfile, System.getenv(RAW_ID_ENV), governedProvider);
    }

    static RoundTripReferenceProvider configuredProvider(
            String deploymentProfile,
            String acknowledgement,
            RoundTripReferenceProvider governedProvider) {
        if (governedProvider == null) {
            throw new IllegalArgumentException("governedProvider must not be null");
        }
        return mode(deploymentProfile, acknowledgement) == Mode.RAW_DIAGNOSTIC
                ? new RawRoundTripReferenceProvider()
                : governedProvider;
    }
}
