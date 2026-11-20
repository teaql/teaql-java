package com.teaql.runtimeexampleconformanceservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.teaql.runtimeexampleconformanceservice.platform.PlatformChecker(), new com.teaql.runtimeexampleconformanceservice.workitem.WorkItemChecker())
      .withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model.fromJson(String.join("", "{\"schema\":\"teaql.checker-model/v1\",\"revision\":\"sha256:f026153870cc54db18171cbbe02db1db174f3dea513e9d21e0ffe8af787ebaed\",\"entities\":{\"Platform\":{\"labels\":{\"en\":\"Platform\"},\"fields\":[{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]},{\"name\":\"work_item_list\",\"targetType\":\"WorkItem\",\"labels\":{\"en\":\"Work Item\"},\"aliases\":[\"WorkItemList\",\"workItemList\"]}]},\"WorkItem\":{\"labels\":{\"en\":\"Work Item\"},\"fields\":[{\"name\":\"description\",\"labels\":{\"en\":\"description\"},\"aliases\":[\"Description\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"platform\",\"targetType\":\"Platform\",\"labels\":{\"en\":\"Platform\"},\"aliases\":[\"Platform\"]},{\"name\":\"title\",\"labels\":{\"en\":\"title\"},\"aliases\":[\"Title\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]}}}")))
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Platform", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("WorkItem", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("title", "title"), java.util.Map.entry("description", "description"), java.util.Map.entry("platform", "platform"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

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
