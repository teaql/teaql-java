package com.example.scalartypeconformanceservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.example.scalartypeconformanceservice.typeplatform.TypePlatformChecker(), new com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbeChecker())
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("TypePlatform", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("ScalarTypeProbe", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("type_platform", "typePlatform"), java.util.Map.entry("required_string", "requiredString"), java.util.Map.entry("optional_string", "optionalString"), java.util.Map.entry("large_text", "largeText"), java.util.Map.entry("required_boolean", "requiredBoolean"), java.util.Map.entry("optional_boolean", "optionalBoolean"), java.util.Map.entry("required_integer", "requiredInteger"), java.util.Map.entry("optional_integer", "optionalInteger"), java.util.Map.entry("required_long", "requiredLong"), java.util.Map.entry("optional_long", "optionalLong"), java.util.Map.entry("required_decimal", "requiredDecimal"), java.util.Map.entry("optional_decimal", "optionalDecimal"), java.util.Map.entry("required_float", "requiredFloat"), java.util.Map.entry("optional_float", "optionalFloat"), java.util.Map.entry("required_double", "requiredDouble"), java.util.Map.entry("optional_double", "optionalDouble"), java.util.Map.entry("required_date", "requiredDate"), java.util.Map.entry("optional_date", "optionalDate"), java.util.Map.entry("required_time", "requiredTime"), java.util.Map.entry("optional_time", "optionalTime"), java.util.Map.entry("required_datetime", "requiredDatetime"), java.util.Map.entry("optional_datetime", "optionalDatetime"), java.util.Map.entry("create_time", "createTime"), java.util.Map.entry("update_time", "updateTime"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.typePlatforms().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.example.scalartypeconformanceservice.typeplatform.TypePlatform domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.example.scalartypeconformanceservice.typeplatform.TypePlatform();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Scalar Type Conformance");
      domainRoot.auditAs("create generated Domain Root TypePlatform").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("TypePlatform", domainRoot.getId()));
  }
}
