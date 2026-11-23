package com.teaql.merchantorderprivacyservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.teaql.merchantorderprivacyservice.merchant.MerchantChecker(), new com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfileChecker(), new com.teaql.merchantorderprivacyservice.customerorder.CustomerOrderChecker())
      .withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model.fromJson(String.join("", "{\"schema\":\"teaql.checker-model/v1\",\"revision\":\"sha256:9136e660d5a681229d56c6670cb24a5831b04ca1677b181ea98574f12221c515\",\"entities\":{\"CustomerOrder\":{\"labels\":{\"en\":\"Customer Order\"},\"fields\":[{\"name\":\"customer_profile\",\"targetType\":\"CustomerProfile\",\"labels\":{\"en\":\"Customer Profile\"},\"aliases\":[\"CustomerProfile\",\"customerProfile\"]},{\"name\":\"fulfillment_status\",\"labels\":{\"en\":\"fulfillment status\"},\"aliases\":[\"FulfillmentStatus\",\"fulfillmentStatus\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"merchant\",\"targetType\":\"Merchant\",\"labels\":{\"en\":\"Merchant\"},\"aliases\":[\"Merchant\"]},{\"name\":\"order_number\",\"labels\":{\"en\":\"order number\"},\"aliases\":[\"OrderNumber\",\"orderNumber\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"CustomerProfile\":{\"labels\":{\"en\":\"Customer Profile\"},\"fields\":[{\"name\":\"customer_order_list\",\"targetType\":\"CustomerOrder\",\"labels\":{\"en\":\"Customer Order\"},\"aliases\":[\"CustomerOrderList\",\"customerOrderList\"]},{\"name\":\"email\",\"labels\":{\"en\":\"email\"},\"aliases\":[\"Email\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"merchant\",\"targetType\":\"Merchant\",\"labels\":{\"en\":\"Merchant\"},\"aliases\":[\"Merchant\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"Merchant\":{\"labels\":{\"en\":\"Merchant\"},\"fields\":[{\"name\":\"customer_order_list\",\"targetType\":\"CustomerOrder\",\"labels\":{\"en\":\"Customer Order\"},\"aliases\":[\"CustomerOrderList\",\"customerOrderList\"]},{\"name\":\"customer_profile_list\",\"targetType\":\"CustomerProfile\",\"labels\":{\"en\":\"Customer Profile\"},\"aliases\":[\"CustomerProfileList\",\"customerProfileList\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]}}}")))
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Merchant", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("CustomerProfile", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("merchant", "merchant"), java.util.Map.entry("email", "email"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("CustomerOrder", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("merchant", "merchant"), java.util.Map.entry("customer_profile", "customerProfile"), java.util.Map.entry("order_number", "orderNumber"), java.util.Map.entry("fulfillment_status", "fulfillmentStatus"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.merchants().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.teaql.merchantorderprivacyservice.merchant.Merchant domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.teaql.merchantorderprivacyservice.merchant.Merchant();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Harbour Retail");
      domainRoot.auditAs("create generated Domain Root Merchant").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Merchant", domainRoot.getId()));
  }
}
