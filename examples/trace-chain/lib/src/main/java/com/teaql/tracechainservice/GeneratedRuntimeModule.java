package com.teaql.tracechainservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.teaql.tracechainservice.platform.PlatformChecker(), new com.teaql.tracechainservice.customerorder.CustomerOrderChecker(), new com.teaql.tracechainservice.orderitem.OrderItemChecker(), new com.teaql.tracechainservice.payment.PaymentChecker(), new com.teaql.tracechainservice.paymentattempt.PaymentAttemptChecker(), new com.teaql.tracechainservice.shipment.ShipmentChecker())
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Platform", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("CustomerOrder", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("platform", "platform"), java.util.Map.entry("order_number", "orderNumber"), java.util.Map.entry("description", "description"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("OrderItem", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("customer_order", "customerOrder"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("Payment", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("customer_order", "customerOrder"), java.util.Map.entry("reference_code", "referenceCode"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("PaymentAttempt", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("payment", "payment"), java.util.Map.entry("reference_code", "referenceCode"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("Shipment", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("customer_order", "customerOrder"), java.util.Map.entry("reference_code", "referenceCode"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.platforms().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.teaql.tracechainservice.platform.Platform domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.teaql.tracechainservice.platform.Platform();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Trace Chain Verification");
      domainRoot.auditAs("create generated Domain Root Platform").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Platform", domainRoot.getId()));
  }
}
