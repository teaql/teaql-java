package io.teaql.autoconfigure;

import io.teaql.core.UserContext;

public interface UserContextFactory {
    UserContext create(Object request);
}
