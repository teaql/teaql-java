package io.teaql.tools;

import io.teaql.core.UserContext;
import io.teaql.tools.spi.ToolProvider;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ContextToolsTest {

    @Test
    public void findsHttpToolThroughRegistry() {
        Tools tools = ContextTools.of(null);

        assertTrue(tools.has(AgentHttpTool.class));
        assertNotNull(tools.get(AgentHttpTool.class));
    }

    @Test(expected = SecurityException.class)
    public void policyCanDenyAvailableTool() {
        Tools tools = ContextTools.builder(null)
                .policy(ToolPolicy.builder().deny(AgentHttpTool.class).build())
                .build();

        tools.get(AgentHttpTool.class);
    }

    @Test(expected = SecurityException.class)
    public void acknowledgementIsRequiredOnlyWhenDescriptorDeclaresIt() {
        Tools tools = ContextTools.builder(null)
                .provider(new DangerousToolProvider())
                .policy(ToolPolicy.builder().allow(DangerousTool.class).build())
                .acknowledgements(ToolAcknowledgements.none())
                .build();

        tools.get(DangerousTool.class);
    }

    @Test
    public void acknowledgementAllowsDangerousToolWhenValueMatches() {
        Tools tools = ContextTools.builder(null)
                .provider(new DangerousToolProvider())
                .policy(ToolPolicy.builder().allow(DangerousTool.class).build())
                .acknowledgements(ToolAcknowledgements.from(Map.of(
                        "TEAQL_TOOL_DANGEROUS_ACK",
                        "__i_understand_this_test_tool_is_dangerous")))
                .build();

        assertNotNull(tools.get(DangerousTool.class));
    }

    interface DangerousTool {
    }

    static final class DangerousToolProvider implements ToolProvider {
        private static final ToolDescriptor DESCRIPTOR = ToolDescriptor
                .builder("dangerous-test", DangerousTool.class)
                .risk(ToolRisk.PRIVILEGED)
                .acknowledgement(
                        "TEAQL_TOOL_DANGEROUS_ACK",
                        "__i_understand_this_test_tool_is_dangerous")
                .build();

        @Override
        public ToolDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public <T> T create(Class<T> toolType, UserContext ctx) {
            return toolType.cast(new DangerousTool() {
            });
        }
    }
}
