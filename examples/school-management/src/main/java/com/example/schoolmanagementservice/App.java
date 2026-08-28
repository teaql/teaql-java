
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
      SpringJdbcSqlExecutor executor, DataSource dataSource) {
      return new io.teaql.core.sqlite.SqliteDataServiceExecutor(
          "default", executor, dataSource);
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
          System.out.println("PASS Java School bootstrap and portable Query parity");
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
}
