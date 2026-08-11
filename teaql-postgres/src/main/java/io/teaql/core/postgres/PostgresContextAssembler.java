package io.teaql.core.postgres;

import io.teaql.core.UserContext;
import io.teaql.core.spi.ContextAssembler;

public class PostgresContextAssembler implements ContextAssembler {
    
    @Override
    public int getOrder() {
        // Run after CoreAssembler (which is 0)
        return 100;
    }

    @Override
    public void initGlobalResources() {
        System.out.println("  -> [PostgresAssembler] Cold Boot: Initializing PostgreSQL Dialect drivers...");
        // Here we would initialize heavy postgres specific resources if any
    }

    @Override
    public void mountTo(UserContext ctx) {
        // Mount postgres dialect info to the current user context
        ctx.putAttribute("DIALECT", "POSTGRES");
        // Append to assembly chain for debugging
        String currentChain = (String) ctx.getObj("ASSEMBLER_CHAIN", "");
        ctx.putAttribute("ASSEMBLER_CHAIN", currentChain + "Postgres->");
    }
}
