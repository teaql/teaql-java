package io.teaql.core.secret;

/** Names a trusted startup binding, never a process variable or filesystem path. */
public record SecretReference(String profile, String alias) {
    public SecretReference {
        if (!valid(profile) || !valid(alias)) {
            throw new SecretFailure(SecretFailure.Code.INVALID_INPUT);
        }
    }

    private static boolean valid(String value) {
        return value != null && value.matches("[A-Za-z][A-Za-z0-9_-]{0,63}");
    }
}
