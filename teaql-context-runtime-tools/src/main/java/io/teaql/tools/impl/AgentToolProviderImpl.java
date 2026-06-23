package io.teaql.tools.impl;

import io.teaql.core.UserContext;
import io.teaql.tools.AgentHttpTool;
import io.teaql.tools.spi.AgentToolProvider;

public class AgentToolProviderImpl implements AgentToolProvider {
    
    @Override
    public AgentHttpTool getHttpTool(UserContext ctx) {
        return new AgentHttpToolImpl(ctx);
    }
}
