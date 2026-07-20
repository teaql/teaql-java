package io.teaql.runtime.boot;

import io.teaql.core.UserContext;

public class TestSPIBoot {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("====== [TeaQL Demo] Server Starting ======");
        
        // Simulating the arrival of Request 1
        System.out.println("\n>>> [Web Server] HTTP Request 1 Arrived");
        System.out.println("  -> System asks for a new UserContext...");
        long start1 = System.nanoTime();
        UserContext ctx1 = TeaQLUserContextFactory.create();
        long end1 = System.nanoTime();
        
        System.out.println("  -> UserContext created in " + (end1 - start1) + " ns");
        System.out.println("  -> [Validate] ctx1 SYSTEM_VERSION: " + ctx1.getStr("SYSTEM_VERSION"));
        System.out.println("  -> [Validate] ctx1 ASSEMBLER_CHAIN: " + ctx1.getStr("ASSEMBLER_CHAIN"));

        System.out.println("\n>>> [Warmup] JVM Warming up...");
        for (int i = 0; i < 10000; i++) {
            TeaQLUserContextFactory.create();
        }

        System.out.println("\n>>> [Benchmark] Creating 1,000,000 UserContexts...");
        int iterations = 1000000;
        long benchmarkStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            UserContext ctx = TeaQLUserContextFactory.create();
            // Optional: avoid dead code elimination
            if (ctx == null) break; 
        }
        long benchmarkEnd = System.nanoTime();
        
        long totalNs = benchmarkEnd - benchmarkStart;
        double avgNs = (double) totalNs / iterations;
        
        System.out.println("  -> Total time for 1M contexts: " + (totalNs / 1_000_000.0) + " ms");
        System.out.println("  -> Average time per context: " + String.format("%.2f", avgNs) + " ns");
        
        System.out.println("\n====== [TeaQL Demo] Server Shutting Down ======");
    }
}
