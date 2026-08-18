package io.teaql.core.postgres;

import io.teaql.core.UserContext;
import io.teaql.runtime.boot.TeaQLUserContextFactory;

public class TestPostgresSPIBoot {
    public static void main(String[] args) {
        System.out.println("====== [TeaQL Postgres Demo] Testing SPI Loader ======");
        
        long start = System.nanoTime();
        UserContext context = TeaQLUserContextFactory.create();
        long end = System.nanoTime();
        
        System.out.println("  -> UserContext created in " + (end - start) + " ns");
        System.out.println("  -> [Validate] ASSEMBLER_CHAIN: " + context.getStr("ASSEMBLER_CHAIN"));
        System.out.println("  -> [Validate] DIALECT Configured: " + context.getStr("DIALECT"));
        
        System.out.println("====== [Test Complete] ======");
        System.exit(0);
    }
}
