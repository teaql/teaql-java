package com.teaql.supplierordersyncservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.teaql.supplierordersyncservice.supplier.SupplierChecker(), new com.teaql.supplierordersyncservice.supplierorder.SupplierOrderChecker())
      .withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model.fromJson(String.join("", "{\"schema\":\"teaql.checker-model/v1\",\"revision\":\"sha256:e586b81a9295abcb4491692dac6cf1ef33839a688fea9a396db189f87f7120c5\",\"entities\":{\"Supplier\":{\"labels\":{\"en\":\"Supplier\"},\"fields\":[{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"supplier_order_list\",\"targetType\":\"SupplierOrder\",\"labels\":{\"en\":\"Supplier Order\"},\"aliases\":[\"SupplierOrderList\",\"supplierOrderList\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"SupplierOrder\":{\"labels\":{\"en\":\"Supplier Order\"},\"fields\":[{\"name\":\"external_reference\",\"labels\":{\"en\":\"external reference\"},\"aliases\":[\"ExternalReference\",\"externalReference\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"supplier\",\"targetType\":\"Supplier\",\"labels\":{\"en\":\"Supplier\"},\"aliases\":[\"Supplier\"]},{\"name\":\"sync_status\",\"labels\":{\"en\":\"sync status\"},\"aliases\":[\"SyncStatus\",\"syncStatus\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]}}}")))
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Supplier", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("SupplierOrder", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("supplier", "supplier"), java.util.Map.entry("external_reference", "externalReference"), java.util.Map.entry("sync_status", "syncStatus"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.suppliers().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.teaql.supplierordersyncservice.supplier.Supplier domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.teaql.supplierordersyncservice.supplier.Supplier();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Regional Supply Co");
      domainRoot.auditAs("create generated Domain Root Supplier").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Supplier", domainRoot.getId()));
  }
}
