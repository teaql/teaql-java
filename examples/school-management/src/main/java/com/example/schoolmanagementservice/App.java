
package com.example.schoolmanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.DataServiceExecutor;
import io.teaql.core.DataServiceRegistry;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.SchemaExecutor;
import io.teaql.core.UserContext;
import io.teaql.core.SmartList;
import io.teaql.provider.springjdbc.SpringJdbcSqlExecutor;
import io.teaql.runtime.TeaQLRuntime;
import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.school.School;
import com.example.schoolmanagementservice.school.SchoolRequest;
import com.example.schoolmanagementservice.schooltype.SchoolType;
import java.time.LocalDate;
import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;

@SpringBootApplication
public class App {

  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }

  @Bean
  public EntityMetaFactory entityMetaFactory() {
      SimpleEntityMetaFactory factory = new SimpleEntityMetaFactory();
      GeneratedRuntimeModule.module().install(factory);
      EntityMetaFactory.registerGlobal(factory);
      return factory;
  }

  @Bean
  public SpringJdbcSqlExecutor springJdbcSqlExecutor(NamedParameterJdbcTemplate jdbcTemplate) {
      return new SpringJdbcSqlExecutor(jdbcTemplate);
  }

  @Bean
  public DataServiceExecutor dataServiceExecutor(
      DataSource dataSource) {
      return new io.teaql.core.sqlite.SqliteDataServiceExecutor(
          "default", new io.teaql.provider.jdbc.JdbcSqlExecutor(dataSource), dataSource);
  }

  @Bean
  public io.teaql.core.DataServiceRegistry dataServiceRegistry(DataServiceExecutor dataServiceExecutor) {
      io.teaql.runtime.DefaultDataServiceRegistry registry = new io.teaql.runtime.DefaultDataServiceRegistry();
      registry.register("default", dataServiceExecutor);
      return registry;
  }

  @Bean
  public WorkspaceTeaQLDatabase teaQLDatabase(SpringJdbcSqlExecutor executor) {
      return new WorkspaceTeaQLDatabase(executor);
  }

  @Bean
  public TeaQLRuntime teaQLRuntime(
      EntityMetaFactory entityMetaFactory,
      DataServiceRegistry registry,
      WorkspaceTeaQLDatabase database) {
      return TeaQLRuntime.builder()
          .metadata(entityMetaFactory)
          .registry(registry)
          .idGenerationService(new IdSpaceIdGenerator(database))
          .build()
          .install(GeneratedRuntimeModule.module());
  }

  @Bean
  public CommandLineRunner teaQLConsoleStartup(
      TeaQLRuntime runtime,
      DataServiceExecutor dataServiceExecutor,
      WorkspaceTeaQLDatabase database) {
      return args -> {
          UserContext context = new CustomUserContext(runtime);
          if (!(dataServiceExecutor instanceof SchemaExecutor schema)) {
              throw new IllegalStateException("default data service has no schema capability");
          }
          context.ensureSchema();
          context.ensureSchema();
          SmartList<Platform> platforms = Q.platforms()
              .comment("verify seeded Platform root")
              .purpose("local runtime verification")
              .executeForList(context);
          SmartList<SchoolType> constants = Q.schoolTypes()
              .orderByIdAscending()
              .comment("verify seeded SchoolType constants")
              .purpose("local runtime verification")
              .executeForList(context);
          require(platforms.size() == 1 && platforms.get(0).getId() == 1L,
              "Platform id 1 was not seeded");
          require(constants.size() == 2
                  && constants.get(0).getId() == 1001L
                  && constants.get(1).getId() == 1002L,
              "SchoolType constants were not seeded");
          require(constants.get(0).getVersion() == 1L && constants.get(1).getVersion() == 1L,
              "Repeated ensureSchema was not idempotent");
          require(new IdSpaceIdGenerator(database).nextId("SchoolType") > 1002L,
              "SchoolType ID floor did not advance beyond model constants");

          SmartList<School> existing = Q.schools()
              .withNameIs("Riverside Primary School")
              .comment("Find the deterministic School query fixture")
              .purpose("Initialize the shared School Query conformance cases")
              .executeForList(context);
          if (existing.isEmpty()) {
              School school = Q.schools()
                  .comment("Create the deterministic School query fixture")
                  .purpose("Initialize the shared School Query conformance cases")
                  .newEntity(context);
              school.updatePlatform(platforms.get(0));
              school.updateSchoolTypeToPrimary();
              school.updateName("Riverside Primary School");
              school.updateAddress("12 River Road, Springfield");
              school.updateEstablishedDate(LocalDate.of(1995, 9, 1));
              school.updateStudentCapacity(800);
              school.updateActive(true);
              school.auditAs("Create the School Query conformance fixture").save(context);
          }

          verifyDynamicSearch(context);
          assertQuery(context, "string equality", Q.schools().withNameIs("Riverside Primary School"), 1);
          assertQuery(context, "string inequality", Q.schools().withNameIsNot("Another School"), 1);
          assertQuery(context, "string membership", Q.schools().withNameIn("Riverside Primary School", "Another School"), 1);
          assertQuery(context, "negative membership", Q.schools().withNameNotIn("Another School"), 1);
          assertQuery(context, "contains", Q.schools().withNameContaining("Primary"), 1);
          assertQuery(context, "negative contains", Q.schools().withNameNotContaining("Secondary"), 1);
          assertQuery(context, "starts with", Q.schools().withNameStartingWith("Riverside"), 1);
          assertQuery(context, "negative starts with", Q.schools().withNameNotStartingWith("Lakeside"), 1);
          assertQuery(context, "ends with", Q.schools().withNameEndingWith("School"), 1);
          assertQuery(context, "negative ends with", Q.schools().withNameNotEndingWith("Academy"), 1);
          assertQuery(context, "number range", Q.schools().withStudentCapacityBetween(700, 900), 1);
          assertQuery(context, "strict comparison", Q.schools().withStudentCapacityGreaterThan(799).withStudentCapacityLessThan(801), 1);
          assertQuery(context, "date range", Q.schools().withEstablishedDateBetween(LocalDate.of(1995, 1, 1), LocalDate.of(1995, 12, 31)), 1);
          assertQuery(context, "known", Q.schools().withAddressIsKnown(), 1);
          assertQuery(context, "unknown", Q.schools().withAddressIsUnknown(), 0);
          assertQuery(context, "boolean true", Q.schools().whichAreActive(), 1);
          assertQuery(context, "boolean false", Q.schools().whichAreNotActive(), 0);
          assertQuery(context, "constant relation", Q.schools().withSchoolTypeIsPrimary(), 1);

          School related = Q.schools()
              .withNameIs("Riverside Primary School")
              .selectPlatformWith(Q.platformsWithMinimalFields().selectName().selectBaseUrl())
              .selectSchoolTypeWith(Q.schoolTypesWithMinimalFields().selectName().selectCode())
              .comment("Query parity: typed forward relations")
              .purpose("Execute the shared School Query conformance case")
              .executeForOne(context);
          require(related != null
                  && related.getPlatform() != null
                  && "Campus Learning Platform".equals(related.getPlatform().getName())
                  && related.getSchoolType() != null
                  && "PRIMARY".equals(related.getSchoolType().getCode()),
              "Typed forward relation query did not hydrate Platform and SchoolType");

          SmartList<School> projected = Q.schools()
              .selectName()
              .orderByIdDescending()
              .comment("Query parity: projection and ordering")
              .purpose("Execute the shared School Query conformance case")
              .executeForList(context);
          require(projected.size() == 1
                  && "Riverside Primary School".equals(projected.get(0).getName()),
              "Projection/order query did not preserve the typed School result");

          SmartList<School> includeAllFacet = Q.schools()
              .withNameContaining("Primary")
              .facetBySchoolTypeAs(
                  "schoolTypeFacet",
                  Q.schoolTypesWithMinimalFields().selectCode().countAs("schoolCount"),
                  true)
              .comment("Facet SchoolType values including zero-count constants")
              .purpose("Verify Java native SQLite Facet semantics")
              .executeForList(context);
          SmartList<SchoolType> allValues = includeAllFacet.getFacet("schoolTypeFacet");
          require(allValues != null && allValues.size() == 2,
              "include-all SchoolType facet did not retain both constants");
          Number primaryCount = allValues.get(0).getDynamicProperty("schoolCount");
          Number secondaryCount = allValues.get(1).getDynamicProperty("schoolCount");
          require("PRIMARY".equals(allValues.get(0).getCode())
                  && primaryCount != null && primaryCount.intValue() == 1,
              "PRIMARY facet count must be 1, got " + primaryCount);
          require("SECONDARY".equals(allValues.get(1).getCode())
                  && secondaryCount != null && secondaryCount.intValue() == 0,
              "SECONDARY facet count must be 0, got " + secondaryCount);

          SmartList<School> matchedOnlyFacet = Q.schools()
              .withNameContaining("Primary")
              .facetBySchoolTypeAs(
                  "schoolTypeFacet",
                  Q.schoolTypesWithMinimalFields().selectCode().countAs("schoolCount"),
                  false)
              .comment("Facet only SchoolType values matched by the outer School filter")
              .purpose("Verify Java native SQLite matched-only Facet semantics")
              .executeForList(context);
          SmartList<SchoolType> matchedValues = matchedOnlyFacet.getFacet("schoolTypeFacet");
          require(matchedValues != null && matchedValues.size() == 1,
              "matched-only SchoolType facet must contain one value");
          require("PRIMARY".equals(matchedValues.get(0).getCode())
                  && ((Number) matchedValues.get(0).getDynamicProperty("schoolCount")).intValue() == 1,
              "matched-only PRIMARY facet count must be 1");

          System.out.println("PASS Java School bootstrap, portable Query, and native SQLite Facet parity");
      };
  }

  private static void assertQuery(
      UserContext context, String label, SchoolRequest<School> request, int expected) {
      SmartList<School> rows = request
          .comment("Query parity: " + label)
          .purpose("Execute the shared School Query conformance case")
          .executeForList(context);
      require(rows.size() == expected,
          label + ": expected " + expected + ", got " + rows.size());
  }

  private static void require(boolean condition, String message) {
      if (!condition) throw new IllegalStateException(message);
  }

  private static void verifyDynamicSearch(UserContext context) {
      var models = java.util.Map.of(
          "School", new io.teaql.query.json.LocalDynamicSearch.Model(
              java.util.Map.of("name", "string"), java.util.Map.of("platform", "Platform")),
          "Platform", new io.teaql.query.json.LocalDynamicSearch.Model(
              java.util.Map.of("name", "string"), java.util.Map.of()));
      String input = """
          {"filter":{"name":"Riverside Primary School","platform.name":"Campus Learning Platform",
            "removed":"SECRET_VALUE","platform.removed":"SECRET_VALUE"},
           "orderBy":[{"field":"removed","direction":"asc"}]}
          """;
      // Authorization input is server-owned, never accepted from the JSON form.
      for (long authorizedPlatform : new long[] {1L, 2L}) {
        for (String searchInput : new String[] {input, "{}"}) {
          SchoolRequest<School> request = Q.schools().withNameIs("Riverside Primary School")
              .withPlatformMatching(Q.platforms().withIdIs(authorizedPlatform));
          request.setSize(2);
          request.orderByIdDescending();
          int hardLimit = request.hardLimit();
          var warnings = new java.util.ArrayList<io.teaql.query.json.LocalDynamicSearch.Warning>();
          io.teaql.query.json.LocalDynamicSearch.merge(request, searchInput, models, filter -> {
              if (!"$eq".equals(filter.operator())) {
                  throw new IllegalArgumentException("This demo binding supports equality only");
              }
              return switch (filter.fieldPath()) {
                  case "name" -> Q.schools().withNameIs(filter.value().textValue()).getSearchCriteria();
                  case "platform.name" -> Q.schools().withPlatformMatching(
                      Q.platforms().withIdIs(authorizedPlatform).withNameIs(filter.value().textValue()))
                      .getSearchCriteria();
                  default -> throw new IllegalArgumentException("Missing trusted demo binding");
              };
          }, order -> { throw new IllegalArgumentException("No demo order binding required"); }, warnings::add);
          SmartList<School> rows = request.comment("what: generated School dynamic search")
              .purpose("why: verify stale fields cannot bypass related authorization").executeForList(context);
          require(rows.size() == (authorizedPlatform == 1L ? 1 : 0),
              "Dynamic search lost its related authorization filter");
          require(warnings.size() == (searchInput.equals("{}") ? 0 : 3)
                  && request.hardLimit() == hardLimit && request.getSize() == 2,
              "Dynamic search warning or limit contract failed");
        }
      }
      System.out.println("PASS Java generated School dynamic search: related scope, drift warnings, typed bindings");
  }
}
