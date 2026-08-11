package io.teaql.runtime.boot;

import io.teaql.core.UserContext;
import io.teaql.core.spi.ContextAssembler;

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
    }

    @Override
    public void mountTo(UserContext ctx) {
        // Mount constant properties to every new UserContext to prove the SPI is assembling it.
        ctx.putAttribute("SYSTEM_VERSION", "TeaQL-1.198-RELEASE");
        ctx.putAttribute("ASSEMBLER_CHAIN", "Core->");
    }
}
