
package com.teaql.runtimeexampleconformanceservice;

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
import io.teaql.core.checker.CheckException;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.provider.springjdbc.SpringJdbcSqlExecutor;
import io.teaql.runtime.TeaQLRuntime;
import com.teaql.runtimeexampleconformanceservice.platform.Platform;
import com.teaql.runtimeexampleconformanceservice.workitem.WorkItem;
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
      TeaQLRuntime runtime, DataServiceExecutor dataServiceExecutor) {
      return args -> {
          UserContext context = new CustomUserContext(runtime);
          if (!(dataServiceExecutor instanceof SchemaExecutor schema)) {
              throw new IllegalStateException("default data service has no schema capability");
          }
          context.ensureSchema();
          System.out.println("PASS ensureSchema (explicit SQLite DDL from Runtime Module)");

          Platform platform = Q.platforms()
              .comment("Load the system-provided platform root")
              .purpose("Run the retained Java conformance example")
              .executeForOne(context);
          require(platform != null, "ensureSchema must provide the platform root");

          long beforeInvalid = Q.workItems()
              .comment("Count valid work items before checker rejection")
              .purpose("Prove an invalid mutation was not persisted")
              .executeForList(context).size();
          WorkItem invalid = Q.workItems()
              .comment("Construct an invalid work item")
              .purpose("Verify Checker rejection before persistence")
              .newEntity(context)
              .updatePlatform(platform);
          try {
              invalid.auditAs("Reject a missing required title").save(context);
              throw new IllegalStateException("Checker accepted a missing title");
          } catch (CheckException expected) {
              require(expected.getViolates().stream().anyMatch(v -> v.toString().contains("title")),
                  "Checker error must identify title");
          }
          long afterInvalid = Q.workItems()
              .comment("Count valid work items after checker rejection")
              .purpose("Prove an invalid mutation was not persisted")
              .executeForList(context).size();
          require(beforeInvalid == afterInvalid, "Checker failure reached persistence");
          System.out.println("PASS Checker (canonical title key, rejected before persistence)");

          WorkItem created = Q.workItems()
              .comment("Construct the conformance work item")
              .purpose("Verify audited create")
              .newEntity(context)
              .updateTitle("Initial title")
              .updatePlatform(platform)
              .auditAs("Create the conformance work item")
              .save(context);
          require(created.getId() != null && created.getVersion() == 1L,
              "Create must return authoritative id/version");
          System.out.printf("PASS Create (id=%d, version=%d)%n", created.getId(), created.getVersion());

          SmartList<WorkItem> listed = Q.workItems()
              .withTitleIs("Initial title")
              .orderByIdAscending()
              .comment("Load the conformance work item")
              .purpose("Verify typed Q list execution")
              .executeForList(context);
          require(listed.size() == 1, "Q API must return one typed work item");
          System.out.println("PASS Q API (typed SmartList<WorkItem>)");

          WorkItem withPlatform = Q.workItems()
              .withTitleIs("Initial title")
              .selectPlatformWith(Q.platforms().selectName())
              .comment("what: load work item with platform")
              .purpose("why: prove generated relation trace inheritance")
              .executeForOne(context);
          require(withPlatform != null && withPlatform.getPlatform() != null
                  && "Runtime Example".equals(withPlatform.getPlatform().getName()),
              "Forward Platform relation was not loaded");
          System.out.println("PASS relation query (typed Platform and inherited trace intent)");

          WorkItem full = listed.get(0);
          require("Initial title".equals(E.workItem(full).getTitle().eval()), "E loaded title mismatch");
          require("N/A".equals(E.workItem(full).getDescription().orIfNull("N/A")),
              "E loaded null fallback mismatch");
          WorkItem minimal = Q.workItemsWithMinimalFields()
              .selectTitle()
              .withTitleIs("Initial title")
              .comment("Load a deliberately partial work item")
              .purpose("Verify E NotLoaded semantics")
              .executeForOne(context);
          try {
              E.workItem(minimal).getDescription().orIfNull("must-not-hide-not-loaded");
              throw new IllegalStateException("E fallback hid NotLoaded");
          } catch (TeaQLNotLoadedException expected) {
              // Expected: null and not-loaded are intentionally different states.
          }
          System.out.println("PASS E API (loaded, null fallback, and not-loaded are distinct)");

          long oldVersion = full.getVersion();
          WorkItem updated = full.updateTitle("Updated title")
              .auditAs("Update the conformance work item")
              .save(context);
          require(updated.getVersion() == oldVersion + 1, "Update must increment version");
          System.out.printf("PASS Update (version %d -> %d)%n", oldVersion, updated.getVersion());

          updated.markForDeletion().auditAs("Delete the conformance work item").save(context);
          SmartList<WorkItem> remaining = Q.workItems()
              .withTitleIs("Updated title")
              .comment("Verify ordinary queries exclude deleted rows")
              .purpose("Verify audited delete")
              .executeForList(context);
          require(remaining.isEmpty(), "Deleted row remains visible to ordinary Q API");
          System.out.println("PASS Delete (default Q excludes deleted rows)");
          System.out.println("PASS Java minimum runtime conformance: 7/7");
      };
  }

  private static void require(boolean condition, String message) {
      if (!condition) {
          throw new IllegalStateException(message);
      }
  }
}
