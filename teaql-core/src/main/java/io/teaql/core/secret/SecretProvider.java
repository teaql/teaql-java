package io.teaql.core.secret;

import io.teaql.core.UserContext;

/** Customer-installable provider SPI; Context is always part of resolution. */
public interface SecretProvider {
    SecretValue resolve(UserContext context, SecretReference reference);
}
