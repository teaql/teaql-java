package io.teaql.tools.spi;

import io.teaql.core.UserContext;
import io.teaql.tools.ToolDescriptor;

public interface ToolProvider {

    ToolDescriptor descriptor();

    default boolean supports(Class<?> toolType) {
        return descriptor().getToolType().equals(toolType);
    }

    <T> T create(Class<T> toolType, UserContext context);
}
