package com.teaql.runtimeexampleconformanceservice;

import com.teaql.runtimeexampleconformanceservice.platform.PlatformChecker;
import com.teaql.runtimeexampleconformanceservice.workitem.WorkItemChecker;
import io.teaql.core.RuntimeModule;

/** Passive generated manifest. Schema changes require an explicit ensureSchema call. */
public final class GeneratedRuntimeModule {
    private static final RuntimeModule MODULE =
            RuntimeModule.of(new EntityMetaRegistry())
                    .withCheckers(new PlatformChecker(), new WorkItemChecker());

    private GeneratedRuntimeModule() {
    }

    public static RuntimeModule module() {
        return MODULE;
    }
}