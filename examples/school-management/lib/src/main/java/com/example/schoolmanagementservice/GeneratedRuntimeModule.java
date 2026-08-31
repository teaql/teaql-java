package com.example.schoolmanagementservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.example.schoolmanagementservice.platform.PlatformChecker(), new com.example.schoolmanagementservice.schooltype.SchoolTypeChecker(), new com.example.schoolmanagementservice.school.SchoolChecker())
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.platforms().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.example.schoolmanagementservice.platform.Platform domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.example.schoolmanagementservice.platform.Platform();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Campus Learning Platform");
      domainRoot.updateBaseUrl("https://campus.example.com");
      domainRoot.auditAs("create generated Domain Root Platform").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Platform", domainRoot.getId()));
    var constantSchoolType1001Rows = Q.schoolTypes().withIdIs(1001L).comment("what: locate generated constant").purpose("why: idempotent runtime bootstrap").executeForList(context);
    if (constantSchoolType1001Rows.isEmpty()) {
      com.example.schoolmanagementservice.schooltype.SchoolType constantSchoolType1001 = new com.example.schoolmanagementservice.schooltype.SchoolType();
      constantSchoolType1001 = io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, constantSchoolType1001, 1001L);
      constantSchoolType1001.updatePlatform(domainRoot);
      constantSchoolType1001.updateName("Primary");
      constantSchoolType1001.updateCode("PRIMARY");
      constantSchoolType1001.updateDisplayOrder(new java.math.BigDecimal("1"));
      constantSchoolType1001.auditAs("create model constant SchoolType(1001)").save(context);
    } else {
      com.example.schoolmanagementservice.schooltype.SchoolType constantSchoolType1001 = constantSchoolType1001Rows.get(0);
      boolean changed = false;
      if (!(java.util.Objects.equals(constantSchoolType1001.getPlatform().getId(), domainRoot.getId()))) { constantSchoolType1001.updatePlatform(domainRoot); changed = true; }
      if (!(java.util.Objects.equals(constantSchoolType1001.getName(), "Primary"))) { constantSchoolType1001.updateName("Primary"); changed = true; }
      if (!(java.util.Objects.equals(constantSchoolType1001.getCode(), "PRIMARY"))) { constantSchoolType1001.updateCode("PRIMARY"); changed = true; }
      if (!((constantSchoolType1001.getDisplayOrder() != null && constantSchoolType1001.getDisplayOrder().compareTo(new java.math.BigDecimal("1")) == 0))) { constantSchoolType1001.updateDisplayOrder(new java.math.BigDecimal("1")); changed = true; }
      if (changed) constantSchoolType1001.auditAs("reconcile model constant SchoolType(1001)").save(context);
    }
    var constantSchoolType1002Rows = Q.schoolTypes().withIdIs(1002L).comment("what: locate generated constant").purpose("why: idempotent runtime bootstrap").executeForList(context);
    if (constantSchoolType1002Rows.isEmpty()) {
      com.example.schoolmanagementservice.schooltype.SchoolType constantSchoolType1002 = new com.example.schoolmanagementservice.schooltype.SchoolType();
      constantSchoolType1002 = io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, constantSchoolType1002, 1002L);
      constantSchoolType1002.updatePlatform(domainRoot);
      constantSchoolType1002.updateName("Secondary");
      constantSchoolType1002.updateCode("SECONDARY");
      constantSchoolType1002.updateDisplayOrder(new java.math.BigDecimal("2"));
      constantSchoolType1002.auditAs("create model constant SchoolType(1002)").save(context);
    } else {
      com.example.schoolmanagementservice.schooltype.SchoolType constantSchoolType1002 = constantSchoolType1002Rows.get(0);
      boolean changed = false;
      if (!(java.util.Objects.equals(constantSchoolType1002.getPlatform().getId(), domainRoot.getId()))) { constantSchoolType1002.updatePlatform(domainRoot); changed = true; }
      if (!(java.util.Objects.equals(constantSchoolType1002.getName(), "Secondary"))) { constantSchoolType1002.updateName("Secondary"); changed = true; }
      if (!(java.util.Objects.equals(constantSchoolType1002.getCode(), "SECONDARY"))) { constantSchoolType1002.updateCode("SECONDARY"); changed = true; }
      if (!((constantSchoolType1002.getDisplayOrder() != null && constantSchoolType1002.getDisplayOrder().compareTo(new java.math.BigDecimal("2")) == 0))) { constantSchoolType1002.updateDisplayOrder(new java.math.BigDecimal("2")); changed = true; }
      if (changed) constantSchoolType1002.auditAs("reconcile model constant SchoolType(1002)").save(context);
    }
  }
}
