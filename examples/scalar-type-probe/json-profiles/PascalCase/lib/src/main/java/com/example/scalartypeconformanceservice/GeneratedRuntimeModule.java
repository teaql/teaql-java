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
        java.util.Map.entry("TypePlatform", java.util.Map.ofEntries(java.util.Map.entry("id", "Id"), java.util.Map.entry("name", "Name"), java.util.Map.entry("version", "Version"))),
        java.util.Map.entry("ScalarTypeProbe", java.util.Map.ofEntries(java.util.Map.entry("id", "Id"), java.util.Map.entry("type_platform", "TypePlatform"), java.util.Map.entry("required_string", "RequiredString"), java.util.Map.entry("optional_string", "OptionalString"), java.util.Map.entry("large_text", "LargeText"), java.util.Map.entry("required_boolean", "RequiredBoolean"), java.util.Map.entry("optional_boolean", "OptionalBoolean"), java.util.Map.entry("required_integer", "RequiredInteger"), java.util.Map.entry("optional_integer", "OptionalInteger"), java.util.Map.entry("required_long", "RequiredLong"), java.util.Map.entry("optional_long", "OptionalLong"), java.util.Map.entry("required_decimal", "RequiredDecimal"), java.util.Map.entry("optional_decimal", "OptionalDecimal"), java.util.Map.entry("required_float", "RequiredFloat"), java.util.Map.entry("optional_float", "OptionalFloat"), java.util.Map.entry("required_double", "RequiredDouble"), java.util.Map.entry("optional_double", "OptionalDouble"), java.util.Map.entry("required_date", "RequiredDate"), java.util.Map.entry("optional_date", "OptionalDate"), java.util.Map.entry("required_time", "RequiredTime"), java.util.Map.entry("optional_time", "OptionalTime"), java.util.Map.entry("required_datetime", "RequiredDatetime"), java.util.Map.entry("optional_datetime", "OptionalDatetime"), java.util.Map.entry("create_time", "CreateTime"), java.util.Map.entry("update_time", "UpdateTime"), java.util.Map.entry("version", "Version")))
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
