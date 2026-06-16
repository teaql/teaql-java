package io.teaql.tools.impl;

import io.teaql.core.UserContext;
import io.teaql.core.spi.AgentToolProvider;
import io.teaql.core.tools.AgentHttpTool;

public class AgentToolProviderImpl implements AgentToolProvider {
    
    @Override
    public AgentHttpTool getHttpTool(UserContext ctx) {
        return new AgentHttpToolImpl(ctx);
    }
}
