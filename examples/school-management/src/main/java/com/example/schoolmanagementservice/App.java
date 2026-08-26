
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
import com.example.schoolmanagementservice.schooltype.SchoolType;
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
          System.out.println("PASS Java School bootstrap with local runtime");
      };
  }

  private static void require(boolean condition, String message) {
      if (!condition) throw new IllegalStateException(message);
  }
}
