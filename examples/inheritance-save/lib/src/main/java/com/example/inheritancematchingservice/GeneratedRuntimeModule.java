package com.example.inheritancematchingservice;

/** Passive generated manifest. Database changes require context.ensureSchema(). */
public final class GeneratedRuntimeModule {
  private static final io.teaql.core.RuntimeModule MODULE = io.teaql.core.RuntimeModule.of(new EntityMetaRegistry())
      .withCheckers(new com.example.inheritancematchingservice.platform.PlatformChecker(), new com.example.inheritancematchingservice.schooltype.SchoolTypeChecker(), new com.example.inheritancematchingservice.school.SchoolChecker(), new com.example.inheritancematchingservice.academy.AcademyChecker(), new com.example.inheritancematchingservice.trainingacademy.TrainingAcademyChecker(), new com.example.inheritancematchingservice.college.CollegeChecker())
      .withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model.fromJson(String.join("", "{\"schema\":\"teaql.checker-model/v1\",\"revision\":\"sha256:43a0ab45b8baa27dfb7d9eccaf6f8ac33e36491d16f13a7817729d8673c0b53a\",\"entities\":{\"Academy\":{\"labels\":{\"en\":\"Academy\"},\"fields\":[{\"name\":\"campus_code\",\"labels\":{\"en\":\"campus code\"},\"aliases\":[\"CampusCode\",\"campusCode\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]}]},\"College\":{\"labels\":{\"en\":\"College\"},\"fields\":[{\"name\":\"degree_level\",\"labels\":{\"en\":\"degree level\"},\"aliases\":[\"DegreeLevel\",\"degreeLevel\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]}]},\"Platform\":{\"labels\":{\"en\":\"Platform\"},\"fields\":[{\"name\":\"base_url\",\"labels\":{\"en\":\"base url\"},\"aliases\":[\"BaseUrl\",\"baseUrl\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"school_list\",\"targetType\":\"School\",\"labels\":{\"en\":\"School\"},\"aliases\":[\"SchoolList\",\"schoolList\"]},{\"name\":\"school_type_list\",\"targetType\":\"SchoolType\",\"labels\":{\"en\":\"School Type\"},\"aliases\":[\"SchoolTypeList\",\"schoolTypeList\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"School\":{\"labels\":{\"en\":\"School\"},\"fields\":[{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"platform\",\"targetType\":\"Platform\",\"labels\":{\"en\":\"Platform\"},\"aliases\":[\"Platform\"]},{\"name\":\"school_type\",\"targetType\":\"SchoolType\",\"labels\":{\"en\":\"School Type\"},\"aliases\":[\"SchoolType\",\"schoolType\"]},{\"name\":\"student_capacity\",\"labels\":{\"en\":\"student capacity\"},\"aliases\":[\"StudentCapacity\",\"studentCapacity\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"SchoolType\":{\"labels\":{\"en\":\"School Type\"},\"fields\":[{\"name\":\"code\",\"labels\":{\"en\":\"code\"},\"aliases\":[\"Code\"]},{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"name\",\"labels\":{\"en\":\"name\"},\"aliases\":[\"Name\"]},{\"name\":\"platform\",\"targetType\":\"Platform\",\"labels\":{\"en\":\"Platform\"},\"aliases\":[\"Platform\"]},{\"name\":\"school_list\",\"targetType\":\"School\",\"labels\":{\"en\":\"School\"},\"aliases\":[\"SchoolList\",\"schoolList\"]},{\"name\":\"version\",\"labels\":{\"en\":\"version\"},\"aliases\":[\"Version\"]}]},\"TrainingAcademy\":{\"labels\":{\"en\":\"Training Academy\"},\"fields\":[{\"name\":\"id\",\"labels\":{\"en\":\"id\"},\"aliases\":[\"Id\"]},{\"name\":\"specialty\",\"labels\":{\"en\":\"specialty\"},\"aliases\":[\"Specialty\"]}]}}}")))
      .withBootstrap(GeneratedRuntimeModule::ensureGeneratedBootstrap);

  private GeneratedRuntimeModule() {}
  public static io.teaql.core.RuntimeModule module() { return MODULE; }

  /** Canonical KSML field to selected JSON wire name, consumed by HTTP/TFP adapters. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldMappings() {
    return java.util.Map.ofEntries(
        java.util.Map.entry("Platform", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("base_url", "baseUrl"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("SchoolType", java.util.Map.ofEntries(java.util.Map.entry("platform", "platform"), java.util.Map.entry("id", "id"), java.util.Map.entry("name", "name"), java.util.Map.entry("code", "code"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("School", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("platform", "platform"), java.util.Map.entry("school_type", "schoolType"), java.util.Map.entry("name", "name"), java.util.Map.entry("student_capacity", "studentCapacity"), java.util.Map.entry("version", "version"))),
        java.util.Map.entry("Academy", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("campus_code", "campusCode"))),
        java.util.Map.entry("TrainingAcademy", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("specialty", "specialty"))),
        java.util.Map.entry("College", java.util.Map.ofEntries(java.util.Map.entry("id", "id"), java.util.Map.entry("degree_level", "degreeLevel")))
    );
  }

  /** Accepted legacy aliases; empty until explicitly declared by the model. */
  public static java.util.Map<String, java.util.Map<String, String>> wireFieldAliases() {
    return java.util.Map.of();
  }

  private static void ensureGeneratedBootstrap(io.teaql.core.UserContext context) {
    var domainRoots = Q.platforms().withIdIs(1L).comment("what: locate generated Domain Root").purpose("why: idempotent runtime bootstrap").executeForList(context);
    com.example.inheritancematchingservice.platform.Platform domainRoot;
    if (domainRoots.isEmpty()) {
      domainRoot = new com.example.inheritancematchingservice.platform.Platform();
      io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, domainRoot, 1L);
      domainRoot.updateName("Inheritance Campus");
      domainRoot.updateBaseUrl("https://inheritance.example.com");
      domainRoot.auditAs("create generated Domain Root Platform").save(context);
    } else { domainRoot = domainRoots.get(0); }
    context.withActiveRoot(new io.teaql.core.ContextEntityRef("Platform", domainRoot.getId()));
    var constantSchoolType1001Rows = Q.schoolTypes().withIdIs(1001L).comment("what: locate generated constant").purpose("why: idempotent runtime bootstrap").executeForList(context);
    if (constantSchoolType1001Rows.isEmpty()) {
      com.example.inheritancematchingservice.schooltype.SchoolType constantSchoolType1001 = new com.example.inheritancematchingservice.schooltype.SchoolType();
      constantSchoolType1001 = io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, constantSchoolType1001, 1001L);
      constantSchoolType1001.updatePlatform(domainRoot);
      constantSchoolType1001.updateName("Primary");
      constantSchoolType1001.updateCode("PRIMARY");
      constantSchoolType1001.auditAs("create model constant SchoolType(1001)").save(context);
    } else {
      com.example.inheritancematchingservice.schooltype.SchoolType constantSchoolType1001 = constantSchoolType1001Rows.get(0);
      boolean changed = false;
      if (!(java.util.Objects.equals(constantSchoolType1001.getPlatform().getId(), domainRoot.getId()))) { constantSchoolType1001.updatePlatform(domainRoot); changed = true; }
      if (!(java.util.Objects.equals(constantSchoolType1001.getName(), "Primary"))) { constantSchoolType1001.updateName("Primary"); changed = true; }
      if (!(java.util.Objects.equals(constantSchoolType1001.getCode(), "PRIMARY"))) { constantSchoolType1001.updateCode("PRIMARY"); changed = true; }
      if (changed) constantSchoolType1001.auditAs("reconcile model constant SchoolType(1001)").save(context);
    }
    var constantSchoolType1002Rows = Q.schoolTypes().withIdIs(1002L).comment("what: locate generated constant").purpose("why: idempotent runtime bootstrap").executeForList(context);
    if (constantSchoolType1002Rows.isEmpty()) {
      com.example.inheritancematchingservice.schooltype.SchoolType constantSchoolType1002 = new com.example.inheritancematchingservice.schooltype.SchoolType();
      constantSchoolType1002 = io.teaql.core.GeneratedSchemaBootstrap.initializeFixedId(context, constantSchoolType1002, 1002L);
      constantSchoolType1002.updatePlatform(domainRoot);
      constantSchoolType1002.updateName("Secondary");
      constantSchoolType1002.updateCode("SECONDARY");
      constantSchoolType1002.auditAs("create model constant SchoolType(1002)").save(context);
    } else {
      com.example.inheritancematchingservice.schooltype.SchoolType constantSchoolType1002 = constantSchoolType1002Rows.get(0);
      boolean changed = false;
      if (!(java.util.Objects.equals(constantSchoolType1002.getPlatform().getId(), domainRoot.getId()))) { constantSchoolType1002.updatePlatform(domainRoot); changed = true; }
      if (!(java.util.Objects.equals(constantSchoolType1002.getName(), "Secondary"))) { constantSchoolType1002.updateName("Secondary"); changed = true; }
      if (!(java.util.Objects.equals(constantSchoolType1002.getCode(), "SECONDARY"))) { constantSchoolType1002.updateCode("SECONDARY"); changed = true; }
      if (changed) constantSchoolType1002.auditAs("reconcile model constant SchoolType(1002)").save(context);
    }
  }
}
