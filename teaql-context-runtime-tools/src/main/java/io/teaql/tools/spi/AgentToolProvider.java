package io.teaql.tools.spi;

import io.teaql.core.UserContext;
import io.teaql.tools.AgentHttpTool;

/**
 * Service Provider Interface (SPI) for resolving Agent capability tools dynamically.
 */
public interface AgentToolProvider {

    /**
     * Get the HTTP Tool capability bound to the given context.
     *
     * @param ctx The user context to bind the audit trail.
     * @return The HTTP tool facade.
     */
    AgentHttpTool getHttpTool(UserContext ctx);
}
