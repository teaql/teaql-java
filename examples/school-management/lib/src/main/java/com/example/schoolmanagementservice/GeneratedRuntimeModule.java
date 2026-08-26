package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.platform.PlatformChecker;
import com.example.schoolmanagementservice.school.SchoolChecker;
import com.example.schoolmanagementservice.schooltype.SchoolTypeChecker;
import io.teaql.core.RuntimeModule;

/** Passive generated manifest. Schema changes require an explicit ensureSchema call. */
public final class GeneratedRuntimeModule {
    private static final RuntimeModule MODULE =
            RuntimeModule.of(new EntityMetaRegistry())
                    .withCheckers(new PlatformChecker(), new SchoolTypeChecker(), new SchoolChecker());

    private GeneratedRuntimeModule() {
    }

    public static RuntimeModule module() {
        return MODULE;
    }
}