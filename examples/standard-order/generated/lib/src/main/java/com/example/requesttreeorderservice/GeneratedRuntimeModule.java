package com.example.requesttreeorderservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.example.requesttreeorderservice.platform.PlatformChecker(), new com.example.requesttreeorderservice.orderstatus.OrderStatusChecker(), new com.example.requesttreeorderservice.customer.CustomerChecker(), new com.example.requesttreeorderservice.customerorder.CustomerOrderChecker(), new com.example.requesttreeorderservice.orderitem.OrderItemChecker())
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Platform", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("OrderStatus", java.util.Map.ofEntries(java.util.Map.entry("platform", "platform"), java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("code", "code"), java.util.Map.entry("color", "color"), java.util.Map.entry("display_order", "displayOrder"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("Customer", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("platform", "platform"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("CustomerOrder", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("customer", "customer"), java.util.Map.entry("status", "status"), java.util.Map.entry("order_number", "orderNumber"), java.util.Map.entry("order_date", "orderDate"), java.util.Map.entry("total_amount", "totalAmount"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("OrderItem", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("customer_order", "customerOrder"), java.util.Map.entry("product_name", "productName"), java.util.Map.entry("quantity", "quantity"), java.util.Map.entry("unit_price", "unitPrice"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.platforms().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.example.requesttreeorderservice.platform.Platform domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.example.requesttreeorderservice.platform.Platform();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Commerce Test Platform");
      domainRoot.auditAs("create generated Domain Root Platform").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Platform", domainRoot.getId()));
    var constantOrderStatus1001Rows = Q.orderStatuses().withIdIs(1001L).comment("what: locate generated constant").purpose("why: idempotent runtime bootstrap").executeForList(context);
    if (constantOrderStatus1001Rows.isEmpty()) {
      com.example.requesttreeorderservice.orderstatus.OrderStatus constantOrderStatus1001 = new com.example.requesttreeorderservice.orderstatus.OrderStatus();
      constantOrderStatus1001 = io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, constantOrderStatus1001, 1001L);
      constantOrderStatus1001.updatePlatform(domainRoot);
      constantOrderStatus1001.updateName("New");
      constantOrderStatus1001.updateCode("NEW");
      constantOrderStatus1001.updateColor("#94A3B8");
      constantOrderStatus1001.updateDisplayOrder(new java.math.BigDecimal("1"));
      constantOrderStatus1001.auditAs("create model constant OrderStatus(1001)").save(context);
    } else {
      com.example.requesttreeorderservice.orderstatus.OrderStatus constantOrderStatus1001 = constantOrderStatus1001Rows.get(0);
      boolean changed = false;
      if (!(java.util.Objects.equals(constantOrderStatus1001.getPlatform().getId(), domainRoot.getId()))) { constantOrderStatus1001.updatePlatform(domainRoot); changed = true; }
      if (!(java.util.Objects.equals(constantOrderStatus1001.getName(), "New"))) { constantOrderStatus1001.updateName("New"); changed = true; }
      if (!(java.util.Objects.equals(constantOrderStatus1001.getCode(), "NEW"))) { constantOrderStatus1001.updateCode("NEW"); changed = true; }
      if (!(java.util.Objects.equals(constantOrderStatus1001.getColor(), "#94A3B8"))) { constantOrderStatus1001.updateColor("#94A3B8"); changed = true; }
      if (!((constantOrderStatus1001.getDisplayOrder() != null && constantOrderStatus1001.getDisplayOrder().compareTo(new java.math.BigDecimal("1")) == 0))) { constantOrderStatus1001.updateDisplayOrder(new java.math.BigDecimal("1")); changed = true; }
      if (changed) constantOrderStatus1001.auditAs("reconcile model constant OrderStatus(1001)").save(context);
    }
    var constantOrderStatus1002Rows = Q.orderStatuses().withIdIs(1002L).comment("what: locate generated constant").purpose("why: idempotent runtime bootstrap").executeForList(context);
    if (constantOrderStatus1002Rows.isEmpty()) {
      com.example.requesttreeorderservice.orderstatus.OrderStatus constantOrderStatus1002 = new com.example.requesttreeorderservice.orderstatus.OrderStatus();
      constantOrderStatus1002 = io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, constantOrderStatus1002, 1002L);
      constantOrderStatus1002.updatePlatform(domainRoot);
      constantOrderStatus1002.updateName("Paid");
      constantOrderStatus1002.updateCode("PAID");
      constantOrderStatus1002.updateColor("#22C55E");
      constantOrderStatus1002.updateDisplayOrder(new java.math.BigDecimal("2"));
      constantOrderStatus1002.auditAs("create model constant OrderStatus(1002)").save(context);
    } else {
      com.example.requesttreeorderservice.orderstatus.OrderStatus constantOrderStatus1002 = constantOrderStatus1002Rows.get(0);
      boolean changed = false;
      if (!(java.util.Objects.equals(constantOrderStatus1002.getPlatform().getId(), domainRoot.getId()))) { constantOrderStatus1002.updatePlatform(domainRoot); changed = true; }
      if (!(java.util.Objects.equals(constantOrderStatus1002.getName(), "Paid"))) { constantOrderStatus1002.updateName("Paid"); changed = true; }
      if (!(java.util.Objects.equals(constantOrderStatus1002.getCode(), "PAID"))) { constantOrderStatus1002.updateCode("PAID"); changed = true; }
      if (!(java.util.Objects.equals(constantOrderStatus1002.getColor(), "#22C55E"))) { constantOrderStatus1002.updateColor("#22C55E"); changed = true; }
      if (!((constantOrderStatus1002.getDisplayOrder() != null && constantOrderStatus1002.getDisplayOrder().compareTo(new java.math.BigDecimal("2")) == 0))) { constantOrderStatus1002.updateDisplayOrder(new java.math.BigDecimal("2")); changed = true; }
      if (changed) constantOrderStatus1002.auditAs("reconcile model constant OrderStatus(1002)").save(context);
    }
    var constantOrderStatus1003Rows = Q.orderStatuses().withIdIs(1003L).comment("what: locate generated constant").purpose("why: idempotent runtime bootstrap").executeForList(context);
    if (constantOrderStatus1003Rows.isEmpty()) {
      com.example.requesttreeorderservice.orderstatus.OrderStatus constantOrderStatus1003 = new com.example.requesttreeorderservice.orderstatus.OrderStatus();
      constantOrderStatus1003 = io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, constantOrderStatus1003, 1003L);
      constantOrderStatus1003.updatePlatform(domainRoot);
      constantOrderStatus1003.updateName("Cancelled");
      constantOrderStatus1003.updateCode("CANCELLED");
      constantOrderStatus1003.updateColor("#EF4444");
      constantOrderStatus1003.updateDisplayOrder(new java.math.BigDecimal("3"));
      constantOrderStatus1003.auditAs("create model constant OrderStatus(1003)").save(context);
    } else {
      com.example.requesttreeorderservice.orderstatus.OrderStatus constantOrderStatus1003 = constantOrderStatus1003Rows.get(0);
      boolean changed = false;
      if (!(java.util.Objects.equals(constantOrderStatus1003.getPlatform().getId(), domainRoot.getId()))) { constantOrderStatus1003.updatePlatform(domainRoot); changed = true; }
      if (!(java.util.Objects.equals(constantOrderStatus1003.getName(), "Cancelled"))) { constantOrderStatus1003.updateName("Cancelled"); changed = true; }
      if (!(java.util.Objects.equals(constantOrderStatus1003.getCode(), "CANCELLED"))) { constantOrderStatus1003.updateCode("CANCELLED"); changed = true; }
      if (!(java.util.Objects.equals(constantOrderStatus1003.getColor(), "#EF4444"))) { constantOrderStatus1003.updateColor("#EF4444"); changed = true; }
      if (!((constantOrderStatus1003.getDisplayOrder() != null && constantOrderStatus1003.getDisplayOrder().compareTo(new java.math.BigDecimal("3")) == 0))) { constantOrderStatus1003.updateDisplayOrder(new java.math.BigDecimal("3")); changed = true; }
      if (changed) constantOrderStatus1003.auditAs("reconcile model constant OrderStatus(1003)").save(context);
    }
  }
}
