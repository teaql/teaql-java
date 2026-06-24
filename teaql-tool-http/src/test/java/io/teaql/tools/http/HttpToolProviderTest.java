package io.teaql.tools.http;

import io.teaql.tools.ContextTools;
import io.teaql.tools.ToolPolicy;
import io.teaql.tools.Tools;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HttpToolProviderTest {

    @Test
    public void discoversHttpToolWhenProviderModuleIsPresent() {
        Tools tools = ContextTools.builder(null)
                .policy(ToolPolicy.builder().allow(AgentHttpTool.class).build())
                .build();

        assertTrue(tools.has(AgentHttpTool.class));
        assertNotNull(tools.get(AgentHttpTool.class));
    }

    @Test(expected = SecurityException.class)
    public void defaultPolicyDoesNotEnableExternalResourceTool() {
        Tools tools = ContextTools.of(null);

        tools.get(AgentHttpTool.class);
    }
}
