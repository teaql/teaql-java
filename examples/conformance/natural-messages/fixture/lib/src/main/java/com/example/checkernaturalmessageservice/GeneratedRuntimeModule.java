package com.example.checkernaturalmessageservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.example.checkernaturalmessageservice.catalog.CatalogChecker(), new com.example.checkernaturalmessageservice.entry.EntryChecker())
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Catalog", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("Entry", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("catalog", "catalog"), java.util.Map.entry("name", "name"), java.util.Map.entry("quantity", "quantity"), java.util.Map.entry("description", "description"), java.util.Map.entry("protected_note", "protectedNote"), java.util.Map.entry("create_time", "createTime"), java.util.Map.entry("update_time", "updateTime"), java.util.Map.entry("version", "version")))
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
      domainRoot.updateName("Validation Catalog");
      domainRoot.auditAs("create generated Domain Root Catalog").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Catalog", domainRoot.getId()));
  }
}
