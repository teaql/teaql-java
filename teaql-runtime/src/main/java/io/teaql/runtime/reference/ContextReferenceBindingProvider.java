package io.teaql.runtime.reference;

import io.teaql.core.UserContext;

/** Produces opaque, trusted binding bytes from the complete runtime context. */
@FunctionalInterface
public interface ContextReferenceBindingProvider {
    byte[] bindingFor(UserContext context);
}
