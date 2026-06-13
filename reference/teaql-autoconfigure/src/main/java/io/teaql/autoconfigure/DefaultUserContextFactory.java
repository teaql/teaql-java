package io.teaql.autoconfigure;

import io.teaql.core.DataConfigProperties;
import io.teaql.core.UserContext;

import io.teaql.core.utils.ReflectUtil;

public class DefaultUserContextFactory implements UserContextFactory {
    private final DataConfigProperties config;

    public DefaultUserContextFactory(DataConfigProperties config) {
        this.config = config;
    }

    @Override
    public UserContext create(Object request) {
        Class<? extends UserContext> contextType = config.getContextClass();
        UserContext userContext = ReflectUtil.newInstanceIfPossible(contextType);
        userContext.init(request);
        return userContext;
    }
}
