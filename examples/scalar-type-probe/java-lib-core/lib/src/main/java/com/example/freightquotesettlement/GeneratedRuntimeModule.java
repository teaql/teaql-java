package com.example.freightquotesettlement;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.example.freightquotesettlement.platform.PlatformChecker(), new com.example.freightquotesettlement.freightquote.FreightQuoteChecker())
      .withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model.fromJson(String.join("", "{\"schema\":\"teaql.checker-model/v1\",\"revision\":\"sha256:3b8da34e762562f0877bfd04865e33c225b4d854c8a5914ea30b4a922d3df3b7\",\"entities\":{\"FreightQuote\":{\"labels\":{\"en\":\"Freight Quote\"},\"fields\":[{\"name\":\"amount\",\"labels\":{\"en\":\"amount\"},\"aliases\":[\"Amount\"]},{\"name\":\"delivery_date\",\"labels\":{\"en\":\"delivery date\"},\"aliases\":[\"DeliveryDate\",\"deliveryDate\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"instructions\",\"labels\":{\"en\":\"instructions\"},\"aliases\":[\"Instructions\"]},{\"name\":\"package_count\",\"labels\":{\"en\":\"package count\"},\"aliases\":[\"PackageCount\",\"packageCount\"]},{\"name\":\"pickup_time\",\"labels\":{\"en\":\"pickup time\"},\"aliases\":[\"PickupTime\",\"pickupTime\"]},{\"name\":\"platform\",\"targetType\":\"Platform\",\"labels\":{\"en\":\"Dispatch Platform\"},\"aliases\":[\"Platform\"]},{\"name\":\"quoted_at\",\"labels\":{\"en\":\"quoted at\"},\"aliases\":[\"QuotedAt\",\"quotedAt\"]},{\"name\":\"reference\",\"labels\":{\"en\":\"reference\"},\"aliases\":[\"Reference\"]},{\"name\":\"shipment_number\",\"labels\":{\"en\":\"shipment number\"},\"aliases\":[\"ShipmentNumber\",\"shipmentNumber\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"Platform\":{\"labels\":{\"en\":\"Dispatch Platform\"},\"fields\":[{\"name\":\"freight_quote_list\",\"targetType\":\"FreightQuote\",\"labels\":{\"en\":\"Freight Quote\"},\"aliases\":[\"FreightQuoteList\",\"freightQuoteList\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]}}}")))
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Platform", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("FreightQuote", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("platform", "platform"), java.util.Map.entry("reference", "reference"), java.util.Map.entry("amount", "amount"), java.util.Map.entry("shipment_number", "shipmentNumber"), java.util.Map.entry("package_count", "packageCount"), java.util.Map.entry("delivery_date", "deliveryDate"), java.util.Map.entry("pickup_time", "pickupTime"), java.util.Map.entry("quoted_at", "quotedAt"), java.util.Map.entry("instructions", "instructions"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.platforms().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.example.freightquotesettlement.platform.Platform domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.example.freightquotesettlement.platform.Platform();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Harbor Freight Desk");
      domainRoot.auditAs("create generated Domain Root Platform").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Platform", domainRoot.getId()));
  }
}
