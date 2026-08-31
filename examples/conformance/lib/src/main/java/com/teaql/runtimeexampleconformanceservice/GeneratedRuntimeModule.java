package com.teaql.runtimeexampleconformanceservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.teaql.runtimeexampleconformanceservice.platform.PlatformChecker(), new com.teaql.runtimeexampleconformanceservice.workitem.WorkItemChecker())
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.platforms().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.teaql.runtimeexampleconformanceservice.platform.Platform domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.teaql.runtimeexampleconformanceservice.platform.Platform();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Runtime Example");
      domainRoot.auditAs("create generated Domain Root Platform").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Platform", domainRoot.getId()));
  }
}
