package com.teaql.checkerfixadvanced;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.teaql.checkerfixadvanced.tenant.TenantChecker(), new com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrderChecker(), new com.teaql.checkerfixadvanced.orderitem.OrderItemChecker())
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Tenant", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("PurchaseOrder", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("tenant", "tenant"), java.util.Map.entry("contact_phone", "contactPhone"), java.util.Map.entry("internal_note", "internalNote"), java.util.Map.entry("create_time", "createTime"), java.util.Map.entry("update_time", "updateTime"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("OrderItem", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("purchase_order", "purchaseOrder"), java.util.Map.entry("tenant", "tenant"), java.util.Map.entry("product_name", "productName"), java.util.Map.entry("unit_price", "unitPrice"), java.util.Map.entry("create_time", "createTime"), java.util.Map.entry("update_time", "updateTime"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.tenants().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.teaql.checkerfixadvanced.tenant.Tenant domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.teaql.checkerfixadvanced.tenant.Tenant();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Tenant Name");
      domainRoot.auditAs("create generated Domain Root Tenant").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Tenant", domainRoot.getId()));
  }
}
