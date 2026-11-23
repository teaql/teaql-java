package com.example.checkernaturalmessageservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.example.checkernaturalmessageservice.catalog.CatalogChecker(), new com.example.checkernaturalmessageservice.entry.EntryChecker())
      .withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model.fromJson(String.join("", "{\"schema\":\"teaql.checker-model/v1\",\"revision\":\"sha256:9ea98ce6f86d4528fa8abed527685e55e825dcb5a695c61871b7aeab0eba3967\",\"entities\":{\"Catalog\":{\"labels\":{\"en\":\"catalog\",\"zh-CN\":\"目录\"},\"fields\":[{\"name\":\"entry_list\",\"targetType\":\"Entry\",\"labels\":{\"en\":\"entry\",\"zh-CN\":\"明细\"},\"aliases\":[\"EntryList\",\"entryList\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\",\"zh-CN\":\"名称\"},\"aliases\":[\"Name\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"Entry\":{\"labels\":{\"en\":\"entry\",\"zh-CN\":\"明细\"},\"fields\":[{\"name\":\"catalog\",\"targetType\":\"Catalog\",\"labels\":{\"en\":\"catalog\",\"zh-CN\":\"目录\"},\"aliases\":[\"Catalog\"]},{\"name\":\"create_time\",\"labels\":{\"en\":\"create time\"},\"aliases\":[\"CreateTime\",\"createTime\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\",\"zh-CN\":\"名称\"},\"aliases\":[\"Name\"]},{\"name\":\"update_time\",\"labels\":{\"en\":\"update time\"},\"aliases\":[\"UpdateTime\",\"updateTime\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]}}}")))
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Catalog", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("Entry", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("catalog", "catalog"), java.util.Map.entry("name", "name"), java.util.Map.entry("create_time", "createTime"), java.util.Map.entry("update_time", "updateTime"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.catalogs().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.example.checkernaturalmessageservice.catalog.Catalog domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.example.checkernaturalmessageservice.catalog.Catalog();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Catalog");
      domainRoot.auditAs("create generated Domain Root Catalog").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Catalog", domainRoot.getId()));
  }
}
