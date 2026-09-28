package io.teaql.core;

/** Trusted compiler metadata, never a query caller's opt-out from field masking. */
public enum SqlParameterLogPolicy {
    UNKNOWN, PLAIN, MASKED, CREDENTIAL
}
