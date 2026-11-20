package com.example.exactdecimalservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.example.exactdecimalservice.platform.PlatformChecker(), new com.example.exactdecimalservice.decimalentry.DecimalEntryChecker())
      .withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model.fromJson(String.join("", "{\"schema\":\"teaql.checker-model/v1\",\"revision\":\"sha256:91221f5024a3f4a9fb2107c03a7807ea332b6a396a608b76abb394898177c010\",\"entities\":{\"DecimalEntry\":{\"labels\":{\"en\":\"Settlement Quote\"},\"fields\":[{\"name\":\"amount\",\"labels\":{\"en\":\"amount\"},\"aliases\":[\"Amount\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"label\",\"labels\":{\"en\":\"label\"},\"aliases\":[\"Label\"]},{\"name\":\"platform\",\"targetType\":\"Platform\",\"labels\":{\"en\":\"Platform\"},\"aliases\":[\"Platform\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"Platform\":{\"labels\":{\"en\":\"Platform\"},\"fields\":[{\"name\":\"decimal_entry_list\",\"targetType\":\"DecimalEntry\",\"labels\":{\"en\":\"Settlement Quote\"},\"aliases\":[\"DecimalEntryList\",\"decimalEntryList\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]}}}")))
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Platform", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("DecimalEntry", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("platform", "platform"), java.util.Map.entry("label", "label"), java.util.Map.entry("amount", "amount"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.platforms().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.example.exactdecimalservice.platform.Platform domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.example.exactdecimalservice.platform.Platform();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Settlement Quote Ledger");
      domainRoot.auditAs("create generated Domain Root Platform").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Platform", domainRoot.getId()));
  }
}
