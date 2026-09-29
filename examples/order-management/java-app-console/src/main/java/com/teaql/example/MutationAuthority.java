package com.teaql.example;

import java.util.Set;

/** Trusted application authority installed in UserContext by server-side code. */
public record MutationAuthority(Set<String> permissions) {
    public MutationAuthority {
        permissions = Set.copyOf(permissions == null ? Set.of() : permissions);
    }

    public boolean permits(String permission) {
        return permissions.contains(permission);
    }
}
