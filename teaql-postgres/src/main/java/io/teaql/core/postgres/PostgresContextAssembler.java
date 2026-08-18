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
    public void mountTo(UserContext context) {
        // Mount postgres dialect info to the current user context
        context.putAttribute("DIALECT", "POSTGRES");
        // Append to assembly chain for debugging
        String currentChain = (String) context.getObj("ASSEMBLER_CHAIN", "");
        context.putAttribute("ASSEMBLER_CHAIN", currentChain + "Postgres->");
    }
}
