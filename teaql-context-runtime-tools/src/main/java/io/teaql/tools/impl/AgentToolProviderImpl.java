package io.teaql.tools.impl;

import io.teaql.core.UserContext;
import io.teaql.tools.AgentHttpTool;
import io.teaql.tools.ToolDescriptor;
import io.teaql.tools.ToolRisk;
import io.teaql.tools.spi.AgentToolProvider;
import io.teaql.tools.spi.ToolProvider;

public class AgentToolProviderImpl implements AgentToolProvider, ToolProvider {
    private static final ToolDescriptor DESCRIPTOR = ToolDescriptor
            .builder("http", AgentHttpTool.class)
            .risk(ToolRisk.EXTERNAL_RESOURCE)
            .build();
    
    @Override
    public AgentHttpTool getHttpTool(UserContext ctx) {
        return new AgentHttpToolImpl(ctx);
    }

    @Override
    public ToolDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public <T> T create(Class<T> toolType, UserContext ctx) {
        if (!supports(toolType)) {
            throw new IllegalArgumentException("Unsupported tool type: " + toolType.getName());
        }
        return toolType.cast(getHttpTool(ctx));
    }
}
