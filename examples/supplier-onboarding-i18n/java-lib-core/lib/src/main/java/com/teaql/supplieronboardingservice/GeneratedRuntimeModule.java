package com.teaql.supplieronboardingservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.teaql.supplieronboardingservice.warehouse.WarehouseChecker(), new com.teaql.supplieronboardingservice.supplier.SupplierChecker())
      .withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model.fromJson(String.join("", "{\"schema\":\"teaql.checker-model/v1\",\"revision\":\"sha256:f4b63b86dd7cad758f18ebc623d0a4976afb15dbe9f61002523f909bb75098b0\",\"entities\":{\"Supplier\":{\"labels\":{\"en\":\"Supplier\",\"zh-CN\":\"供应商\"},\"fields\":[{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"minimum_order_quantity\",\"labels\":{\"en\":\"minimum order quantity\"},\"aliases\":[\"MinimumOrderQuantity\",\"minimumOrderQuantity\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]},{\"name\":\"warehouse\",\"targetType\":\"Warehouse\",\"labels\":{\"en\":\"Warehouse\",\"zh-CN\":\"仓库\"},\"aliases\":[\"Warehouse\"]}]},\"Warehouse\":{\"labels\":{\"en\":\"Warehouse\",\"zh-CN\":\"仓库\"},\"fields\":[{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"supplier_list\",\"targetType\":\"Supplier\",\"labels\":{\"en\":\"Supplier\",\"zh-CN\":\"供应商\"},\"aliases\":[\"SupplierList\",\"supplierList\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]}}}")))
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Warehouse", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("Supplier", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("warehouse", "warehouse"), java.util.Map.entry("name", "name"), java.util.Map.entry("minimum_order_quantity", "minimumOrderQuantity"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.warehouses().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.teaql.supplieronboardingservice.warehouse.Warehouse domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.teaql.supplieronboardingservice.warehouse.Warehouse();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Central Warehouse");
      domainRoot.auditAs("create generated Domain Root Warehouse").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Warehouse", domainRoot.getId()));
  }
}
