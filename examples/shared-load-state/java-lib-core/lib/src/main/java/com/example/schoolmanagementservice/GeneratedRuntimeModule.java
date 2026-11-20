package com.example.schoolmanagementservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.example.schoolmanagementservice.platform.PlatformChecker(), new com.example.schoolmanagementservice.schooltype.SchoolTypeChecker(), new com.example.schoolmanagementservice.school.SchoolChecker(), new com.example.schoolmanagementservice.schoolcapacitysummary.SchoolCapacitySummaryChecker())
      .withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model.fromJson(String.join("", "{\"schema\":\"teaql.checker-model/v1\",\"revision\":\"sha256:d36b1e0d50365a8a5a3654777b969de3e9e0de9673b6997e9522858ac7239249\",\"entities\":{\"Platform\":{\"labels\":{\"en\":\"platform\"},\"fields\":[{\"name\":\"base_url\",\"labels\":{\"en\":\"base url\"},\"aliases\":[\"BaseUrl\",\"baseUrl\"]},{\"name\":\"create_time\",\"labels\":{\"en\":\"create time\"},\"aliases\":[\"CreateTime\",\"createTime\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"school_capacity_summary_list\",\"targetType\":\"SchoolCapacitySummary\",\"labels\":{\"en\":\"School Capacity Summary\"},\"aliases\":[\"SchoolCapacitySummaryList\",\"schoolCapacitySummaryList\"]},{\"name\":\"school_list\",\"targetType\":\"School\",\"labels\":{\"en\":\"school\"},\"aliases\":[\"SchoolList\",\"schoolList\"]},{\"name\":\"school_type_list\",\"targetType\":\"SchoolType\",\"labels\":{\"en\":\"school type\"},\"aliases\":[\"SchoolTypeList\",\"schoolTypeList\"]},{\"name\":\"update_time\",\"labels\":{\"en\":\"update time\"},\"aliases\":[\"UpdateTime\",\"updateTime\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"School\":{\"labels\":{\"en\":\"school\"},\"fields\":[{\"name\":\"active\",\"labels\":{\"en\":\"active\"},\"aliases\":[\"Active\"]},{\"name\":\"address\",\"labels\":{\"en\":\"address\"},\"aliases\":[\"Address\"]},{\"name\":\"create_time\",\"labels\":{\"en\":\"create time\"},\"aliases\":[\"CreateTime\",\"createTime\"]},{\"name\":\"established_date\",\"labels\":{\"en\":\"established date\"},\"aliases\":[\"EstablishedDate\",\"establishedDate\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"platform\",\"targetType\":\"Platform\",\"labels\":{\"en\":\"platform\"},\"aliases\":[\"Platform\"]},{\"name\":\"school_type\",\"targetType\":\"SchoolType\",\"labels\":{\"en\":\"school type\"},\"aliases\":[\"SchoolType\",\"schoolType\"]},{\"name\":\"student_capacity\",\"labels\":{\"en\":\"student capacity\"},\"aliases\":[\"StudentCapacity\",\"studentCapacity\"]},{\"name\":\"update_time\",\"labels\":{\"en\":\"update time\"},\"aliases\":[\"UpdateTime\",\"updateTime\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"SchoolCapacitySummary\":{\"labels\":{\"en\":\"School Capacity Summary\"},\"fields\":[{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"platform\",\"targetType\":\"Platform\",\"labels\":{\"en\":\"platform\"},\"aliases\":[\"Platform\"]},{\"name\":\"school_count\",\"labels\":{\"en\":\"school count\"},\"aliases\":[\"SchoolCount\",\"schoolCount\"]},{\"name\":\"total_capacity\",\"labels\":{\"en\":\"total capacity\"},\"aliases\":[\"TotalCapacity\",\"totalCapacity\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"SchoolType\":{\"labels\":{\"en\":\"school type\"},\"fields\":[{\"name\":\"code\",\"labels\":{\"en\":\"code\"},\"aliases\":[\"Code\"]},{\"name\":\"display_order\",\"labels\":{\"en\":\"display order\"},\"aliases\":[\"DisplayOrder\",\"displayOrder\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"platform\",\"targetType\":\"Platform\",\"labels\":{\"en\":\"platform\"},\"aliases\":[\"Platform\"]},{\"name\":\"school_list\",\"targetType\":\"School\",\"labels\":{\"en\":\"school\"},\"aliases\":[\"SchoolList\",\"schoolList\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]}}}")))
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Platform", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("base_url", "baseUrl"), java.util.Map.entry("create_time", "createTime"), java.util.Map.entry("update_time", "updateTime"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("SchoolType", java.util.Map.ofEntries(java.util.Map.entry("platform", "platform"), java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("code", "code"), java.util.Map.entry("display_order", "displayOrder"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("School", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("platform", "platform"), java.util.Map.entry("school_type", "schoolType"), java.util.Map.entry("name", "name"), java.util.Map.entry("address", "address"), java.util.Map.entry("established_date", "establishedDate"), java.util.Map.entry("student_capacity", "studentCapacity"), java.util.Map.entry("active", "active"), java.util.Map.entry("create_time", "createTime"), java.util.Map.entry("update_time", "updateTime"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("SchoolCapacitySummary", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("platform", "platform"), java.util.Map.entry("name", "name"), java.util.Map.entry("total_capacity", "totalCapacity"), java.util.Map.entry("school_count", "schoolCount"), java.util.Map.entry("version", "version")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

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
