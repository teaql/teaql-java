package io.teaql.runtime.boot;

import io.teaql.core.UserContext;
import io.teaql.core.spi.ContextAssembler;
import io.teaql.runtime.DefaultUserContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The global bootstrapper and UserContext factory for TeaQL.
 * This class coordinates the SPI ContextAssemblers to construct UserContext instances.
 */
public class TeaQLUserContextFactory {

    private static final List<ContextAssembler> assemblers = new ArrayList<>();
    private static volatile boolean isBootstrapped = false;
    private static final Object lock = new Object();

    /**
     * Private constructor to prevent instantiation.
     */
    private TeaQLUserContextFactory() {}

    /**
     * Cold start: Scan the classpath for all SPI ContextAssemblers, sort them by priority,
     * and trigger their global resource initialization once.
     */
    public static void bootstrap() {
        if (isBootstrapped) {
            return;
        }
        synchronized (lock) {
            if (isBootstrapped) {
                return;
            }
            
            System.out.println("[TeaQL] Bootstrapping Unified Runtime Environment...");
            ServiceLoader<ContextAssembler> loader = ServiceLoader.load(ContextAssembler.class);
            for (ContextAssembler assembler : loader) {
                assemblers.add(assembler);
            }
            
            // Sort them according to getOrder() priority
            Collections.sort(assemblers);
            
            for (ContextAssembler assembler : assemblers) {
                System.out.println("[TeaQL] Initializing Assembler: " + assembler.getClass().getSimpleName() + " (Order: " + assembler.getOrder() + ")");
                assembler.initGlobalResources();
            }
            
            isBootstrapped = true;
            System.out.println("[TeaQL] Bootstrapping completed successfully.");
        }
    }

    /**
     * Creates a new, extremely lightweight UserContext for the current execution/request.
     * This iterates over the sorted SPI Assemblers and allows them to mount their
     * global references onto the new context in O(N) nanosecond operations.
     *
     * @return A fully assembled UserContext
     */
    public static UserContext create() {
        if (!isBootstrapped) {
            bootstrap();
        }
        
        UserContext ctx = new DefaultUserContext(null);
        for (ContextAssembler assembler : assemblers) {
            assembler.mountTo(ctx);
        }
        
        return ctx;
    }
}
