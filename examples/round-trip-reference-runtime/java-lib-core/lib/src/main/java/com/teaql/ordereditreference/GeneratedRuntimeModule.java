package com.teaql.ordereditreference;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.teaql.ordereditreference.merchant.MerchantChecker(), new com.teaql.ordereditreference.salesorder.SalesOrderChecker(), new com.teaql.ordereditreference.orderitem.OrderItemChecker())
      .withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model.fromJson(String.join("", "{\"schema\":\"teaql.checker-model/v1\",\"revision\":\"sha256:c2dacaf8377a726fc38a9cf13920679bb314d4127c7cc9c550dc98d516508240\",\"entities\":{\"Merchant\":{\"labels\":{\"en\":\"merchant\"},\"fields\":[{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"sales_order_list\",\"targetType\":\"SalesOrder\",\"labels\":{\"en\":\"sales order\"},\"aliases\":[\"SalesOrderList\",\"salesOrderList\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"OrderItem\":{\"labels\":{\"en\":\"order item\"},\"fields\":[{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"quantity\",\"labels\":{\"en\":\"quantity\"},\"aliases\":[\"Quantity\"]},{\"name\":\"sales_order\",\"targetType\":\"SalesOrder\",\"labels\":{\"en\":\"sales order\"},\"aliases\":[\"SalesOrder\",\"salesOrder\"]},{\"name\":\"sku\",\"labels\":{\"en\":\"sku\"},\"aliases\":[\"Sku\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"SalesOrder\":{\"labels\":{\"en\":\"sales order\"},\"fields\":[{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"merchant\",\"targetType\":\"Merchant\",\"labels\":{\"en\":\"merchant\"},\"aliases\":[\"Merchant\"]},{\"name\":\"order_item_list\",\"targetType\":\"OrderItem\",\"labels\":{\"en\":\"order item\"},\"aliases\":[\"OrderItemList\",\"orderItemList\"]},{\"name\":\"order_number\",\"labels\":{\"en\":\"order number\"},\"aliases\":[\"OrderNumber\",\"orderNumber\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]}}}")))
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Merchant", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("SalesOrder", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("merchant", "merchant"), java.util.Map.entry("order_number", "orderNumber"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("OrderItem", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("sales_order", "salesOrder"), java.util.Map.entry("sku", "sku"), java.util.Map.entry("quantity", "quantity"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.merchants().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.teaql.ordereditreference.merchant.Merchant domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.teaql.ordereditreference.merchant.Merchant();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Northwind Retail");
      domainRoot.auditAs("create generated Domain Root Merchant").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Merchant", domainRoot.getId()));
  }
}
