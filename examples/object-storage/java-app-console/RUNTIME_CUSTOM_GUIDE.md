
# TeaQL Runtime Customization Guide

This is a plain Java console application. `App.main()` creates and closes the Hikari
DataSource, wraps it with `JdbcSqlExecutor`, and manually assembles `TeaQLRuntime.builder()`.
The generated runtime module installs metadata and runtime services; `CustomUserContext`
is then created explicitly and `context.ensureSchema()` initializes the database.

Run `make run` to install the generated library, package the application, and launch it.
Configuration is loaded from `src/main/resources/application.properties`. Override values
with JVM properties or `--teaql.datasource.url=jdbc:...` command-line arguments.
Server database credentials use `TEAQL_DATABASE_USERNAME` and `TEAQL_DATABASE_PASSWORD`.
Keep business operations in the generated Q/E APIs and audited entity saves.

> [!WARNING]
> **DO NOT GUESS RUNTIME CONFIGURATION**
> Do not guess how to configure the runtime, connection pools, or schema generation.

To get the exact usage and examples for Customizing the Runtime (UserContext assembly, SQL debugging, Schema modes), you must fetch the dynamically generated prompt directly from the code generation server. Use your tools to execute the following command:

```bash
cargo teaql --input models/order-attachment-service.xml java-assist-runtime-custom
```

Once the command succeeds, read its output. Use the printed code as a template to write your logic.