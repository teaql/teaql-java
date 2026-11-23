
package com.example.freightquotesettlement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Properties;

import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.DataServiceExecutor;
import io.teaql.core.DataServiceRegistry;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.UserContext;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.TeaQLRuntime;
import javax.sql.DataSource;

public class App {

  public static void main(String[] args) throws Exception {
      Properties properties = new Properties();
      try (var input = App.class.getResourceAsStream("/application.properties")) {
          if (input != null) properties.load(input);
      }
      for (String arg : args) {
          if (!arg.startsWith("--") || !arg.contains("=")) {
              throw new IllegalArgumentException("Expected --key=value argument");
          }
          int separator = arg.indexOf('=');
          properties.setProperty(arg.substring(2, separator), arg.substring(separator + 1));
      }
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(setting(properties, "teaql.datasource.url"));
      String driver = setting(properties, "teaql.datasource.driver-class-name");
      if (driver != null) config.setDriverClassName(driver);
      String username = setting(properties, "teaql.datasource.username");
      String password = setting(properties, "teaql.datasource.password");
      if (username != null) config.setUsername(username);
      if (password != null) config.setPassword(password);
      try (HikariDataSource source = new HikariDataSource(config)) {
          App app = new App();
          JdbcSqlExecutor executor = new JdbcSqlExecutor(source);
          TeaQLRuntime runtime = app.teaQLRuntime(app.entityMetaFactory(),
              app.dataServiceRegistry(app.dataServiceExecutor(executor, source)),
              app.teaQLDatabase(executor, source));
          UserContext context = new CustomUserContext(runtime);
          context.ensureSchema();
          System.out.println("TeaQL schema ensured explicitly.");
          System.out.println("TeaQL Console Application Started!");
          // Add your CLI logic here, using Q/E and context.
      }
  }

  private static String setting(Properties properties, String key) {
      String value = System.getProperty(key, properties.getProperty(key));
      if (value != null && value.startsWith("${") && value.endsWith("}")) {
          String variable = value.substring(2, value.length() - 1);
          value = System.getenv(variable);
          if (value == null) throw new IllegalStateException("Missing environment variable: " + variable);
      }
      return value;
  }

  public EntityMetaFactory entityMetaFactory() {
      SimpleEntityMetaFactory factory = new SimpleEntityMetaFactory();
      GeneratedRuntimeModule.module().install(factory);
      EntityMetaFactory.registerGlobal(factory);
      return factory;
  }

  public DataServiceExecutor dataServiceExecutor(
      JdbcSqlExecutor executor, DataSource dataSource) {
      return new io.teaql.core.sqlite.SqliteDataServiceExecutor(
          "default", executor, dataSource);
  }

  public io.teaql.core.DataServiceRegistry dataServiceRegistry(DataServiceExecutor dataServiceExecutor) {
      io.teaql.runtime.DefaultDataServiceRegistry registry = new io.teaql.runtime.DefaultDataServiceRegistry();
      registry.register("default", dataServiceExecutor);
      return registry;
  }

  public WorkspaceTeaQLDatabase teaQLDatabase(JdbcSqlExecutor executor, javax.sql.DataSource dataSource) {
      return new WorkspaceTeaQLDatabase(executor, dataSource);
  }

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

}