package io.teaql.core.spi;

import io.teaql.core.UserContext;
import io.teaql.core.tools.AgentHttpTool;

/**
 * Service Provider Interface (SPI) for resolving Agent capability tools dynamically.
 * This ensures core remains decoupled from tool implementation via JPMS.
 */
public interface AgentToolProvider {
    
    /**
     * Get the HTTP Tool capability bound to the given context.
     * @param ctx The user context to bind the audit trail.
     * @return The HTTP tool facade.
     */
    AgentHttpTool getHttpTool(UserContext ctx);
}
