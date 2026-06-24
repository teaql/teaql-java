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
    public void findsRegisteredMemoryToolThroughRegistry() {
        Tools tools = ContextTools.builder(null)
                .provider(new MemoryToolProvider())
                .build();

        assertTrue(tools.has(MemoryTool.class));
        assertNotNull(tools.get(MemoryTool.class));
    }

    @Test(expected = SecurityException.class)
    public void policyCanDenyAvailableTool() {
        Tools tools = ContextTools.builder(null)
                .provider(new MemoryToolProvider())
                .policy(ToolPolicy.builder().deny(MemoryTool.class).build())
                .build();

        tools.get(MemoryTool.class);
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

    interface MemoryTool {
    }

    static final class MemoryToolProvider implements ToolProvider {
        private static final ToolDescriptor DESCRIPTOR = ToolDescriptor
                .builder("memory-test", MemoryTool.class)
                .build();

        @Override
        public ToolDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public <T> T create(Class<T> toolType, UserContext ctx) {
            return toolType.cast(new MemoryTool() {
            });
        }
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
