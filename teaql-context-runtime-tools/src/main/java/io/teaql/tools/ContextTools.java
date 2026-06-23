package io.teaql.tools;

import io.teaql.core.UserContext;
import io.teaql.tools.spi.AgentToolProvider;

import java.util.ServiceLoader;

public final class ContextTools {

    private ContextTools() {
    }

    public static AgentHttpTool http(UserContext ctx) {
        return ServiceLoader.load(AgentToolProvider.class)
                .findFirst()
                .map(provider -> provider.getHttpTool(ctx))
                .orElseThrow(() -> new IllegalStateException("AgentToolProvider implementation not found on classpath/modulepath. Please add teaql-context-runtime-tools module."));
    }
}
