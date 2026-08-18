package io.teaql.tools.http.impl;

import io.teaql.core.UserContext;
import io.teaql.tools.ToolDescriptor;
import io.teaql.tools.ToolRisk;
import io.teaql.tools.http.AgentHttpTool;
import io.teaql.tools.spi.ToolProvider;

public class HttpToolProvider implements ToolProvider {
    private static final ToolDescriptor DESCRIPTOR = ToolDescriptor
            .builder("http", AgentHttpTool.class)
            .risk(ToolRisk.EXTERNAL_RESOURCE)
            .build();

    @Override
    public ToolDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public <T> T create(Class<T> toolType, UserContext context) {
        if (!supports(toolType)) {
            throw new IllegalArgumentException("Unsupported tool type: " + toolType.getName());
        }
        return toolType.cast(new JdkHttpTool(context));
    }
}
