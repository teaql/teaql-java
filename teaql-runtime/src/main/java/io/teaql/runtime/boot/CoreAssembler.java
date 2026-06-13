package io.teaql.runtime.boot;

import io.teaql.core.UserContext;
import io.teaql.core.spi.ContextAssembler;
import io.teaql.runtime.log.LogManager;

/**
 * The fundamental assembler that comes built-in with teaql-runtime.
 * It has the lowest order (0) so it executes first.
 */
public class CoreAssembler implements ContextAssembler {

    @Override
    public int getOrder() {
        return 0; // Highest priority, executes first
    }

    @Override
    public void initGlobalResources() {
        System.out.println("  -> [CoreAssembler] Cold Boot: Initializing global core components...");
        // This forces LogManager to instantiate and boot up its background threads ONLY ONCE
        LogManager.getInstance();
        System.out.println("  -> [CoreAssembler] Cold Boot: LogManager engine started.");
    }

    @Override
    public void mountTo(UserContext ctx) {
        // Mount constant properties to every new UserContext to prove the SPI is assembling it.
        ctx.put("SYSTEM_VERSION", "TeaQL-1.198-RELEASE");
        ctx.put("FEATURE_LOGGING", "ENABLED");
        ctx.put("ASSEMBLER_CHAIN", "Core->");
    }
}
