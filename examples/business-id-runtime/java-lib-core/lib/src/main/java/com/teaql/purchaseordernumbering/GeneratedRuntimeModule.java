package com.teaql.purchaseordernumbering;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.teaql.purchaseordernumbering.platform.PlatformChecker(), new com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrderChecker())
      .withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model.fromJson(String.join("", "{\"schema\":\"teaql.checker-model/v1\",\"revision\":\"sha256:304c22e5b2a924d8fc32148174ef6d249857e8080ea8459c2cb4d8a6fd75ae60\",\"entities\":{\"Platform\":{\"labels\":{\"en\":\"platform\"},\"fields\":[{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"purchase_order_list\",\"targetType\":\"PurchaseOrder\",\"labels\":{\"en\":\"purchase order\"},\"aliases\":[\"PurchaseOrderList\",\"purchaseOrderList\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"PurchaseOrder\":{\"labels\":{\"en\":\"purchase order\"},\"fields\":[{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"order_number\",\"labels\":{\"en\":\"order number\"},\"aliases\":[\"OrderNumber\",\"orderNumber\"]},{\"name\":\"platform\",\"targetType\":\"Platform\",\"labels\":{\"en\":\"platform\"},\"aliases\":[\"Platform\"]},{\"name\":\"supplier\",\"labels\":{\"en\":\"supplier\"},\"aliases\":[\"Supplier\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]}}}")))
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Platform", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("PurchaseOrder", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("platform", "platform"), java.util.Map.entry("supplier", "supplier"), java.util.Map.entry("order_number", "orderNumber"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.platforms().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.teaql.purchaseordernumbering.platform.Platform domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.teaql.purchaseordernumbering.platform.Platform();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Procurement");
      domainRoot.auditAs("create generated Domain Root Platform").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Platform", domainRoot.getId()));
  }
}
