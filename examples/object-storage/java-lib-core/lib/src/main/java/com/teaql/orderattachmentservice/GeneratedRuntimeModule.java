package com.teaql.orderattachmentservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.teaql.orderattachmentservice.commerceplatform.CommercePlatformChecker(), new com.teaql.orderattachmentservice.customerorder.CustomerOrderChecker(), new com.teaql.orderattachmentservice.orderattachment.OrderAttachmentChecker())
      .withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model.fromJson(String.join("", "{\"schema\":\"teaql.checker-model/v1\",\"revision\":\"sha256:f6fb4b9ca6d876325850fdce5a0d2ab5c57f19c74af6bbffb2b494779e9c27b9\",\"entities\":{\"CommercePlatform\":{\"labels\":{\"en\":\"commerce platform\"},\"fields\":[{\"name\":\"customer_order_list\",\"targetType\":\"CustomerOrder\",\"labels\":{\"en\":\"customer order\"},\"aliases\":[\"CustomerOrderList\",\"customerOrderList\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"CustomerOrder\":{\"labels\":{\"en\":\"customer order\"},\"fields\":[{\"name\":\"commerce_platform\",\"targetType\":\"CommercePlatform\",\"labels\":{\"en\":\"commerce platform\"},\"aliases\":[\"CommercePlatform\",\"commercePlatform\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"order_attachment_list\",\"targetType\":\"OrderAttachment\",\"labels\":{\"en\":\"order attachment\"},\"aliases\":[\"OrderAttachmentList\",\"orderAttachmentList\"]},{\"name\":\"order_number\",\"labels\":{\"en\":\"order number\"},\"aliases\":[\"OrderNumber\",\"orderNumber\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"OrderAttachment\":{\"labels\":{\"en\":\"order attachment\"},\"fields\":[{\"name\":\"content_length\",\"labels\":{\"en\":\"content length\"},\"aliases\":[\"ContentLength\",\"contentLength\"]},{\"name\":\"content_sha256\",\"labels\":{\"en\":\"content sha256\"},\"aliases\":[\"ContentSha256\",\"contentSha256\"]},{\"name\":\"content_type\",\"labels\":{\"en\":\"content type\"},\"aliases\":[\"ContentType\",\"contentType\"]},{\"name\":\"customer_order\",\"targetType\":\"CustomerOrder\",\"labels\":{\"en\":\"customer order\"},\"aliases\":[\"CustomerOrder\",\"customerOrder\"]},{\"name\":\"file_name\",\"labels\":{\"en\":\"file name\"},\"aliases\":[\"FileName\",\"fileName\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"object_key\",\"labels\":{\"en\":\"object key\"},\"aliases\":[\"ObjectKey\",\"objectKey\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]}}}")))
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("CommercePlatform", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("CustomerOrder", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("order_number", "orderNumber"), java.util.Map.entry("commerce_platform", "commercePlatform"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("OrderAttachment", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("customer_order", "customerOrder"), java.util.Map.entry("file_name", "fileName"), java.util.Map.entry("content_type", "contentType"), java.util.Map.entry("content_length", "contentLength"), java.util.Map.entry("content_sha256", "contentSha256"), java.util.Map.entry("object_key", "objectKey"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.commercePlatforms().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.teaql.orderattachmentservice.commerceplatform.CommercePlatform domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.teaql.orderattachmentservice.commerceplatform.CommercePlatform();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Order Attachments");
      domainRoot.auditAs("create generated Domain Root CommercePlatform").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("CommercePlatform", domainRoot.getId()));
  }
}
