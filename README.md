# TeaQL Java

[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/13612/badge)](https://www.bestpractices.dev/projects/13612)

TeaQL Java is the Java runtime for TeaQL domain applications. It provides a
typed entity and request model, auditable execution, pluggable runtime
capabilities, portable SQL support, database dialects, and integrations for
server-side Java and Android.

TeaQL is designed for applications in which code may be written or operated by
both humans and coding agents. Instead of exposing unrestricted infrastructure
operations, the runtime keeps execution behind explicit context, intent, policy,
and capability boundaries.

## Why TeaQL?

TeaQL applies five safeguards to application operations:

1. **Context-bound execution** — reads and writes run through a `UserContext`,
   which carries identity, trace, and runtime capabilities.
2. **Declared intent** — reads require `.comment(...).purpose(...)`; writes use
   `.auditAs(...)` before an execution terminal becomes available.
3. **Policy gates** — `RequestPolicy` can inspect or reject select, insert,
   update, delete, and recover operations.
4. **Explicit capabilities** — optional operations such as HTTP tools, dynamic
   fields, and business ID generation are supplied through dedicated modules and
   registered runtime capabilities.
5. **Typed graph mutation** — applications persist typed entity graphs instead
   of assembling ad hoc update statements and relationship loops.

The runtime also records execution metadata through a pluggable
`RuntimeLogSink`, allowing applications to choose their own audit and logging
backend.

## Quick Start

### Requirements

- Java 17 or later
- Maven 3.8 or later

### Spring Boot

Spring Boot applications can use the compatibility starter. Keep the TeaQL
version in one property so all TeaQL artifacts stay aligned:

```xml
<properties>
    <teaql.version>1.525-RELEASE</teaql.version>
</properties>

<dependencies>
    <dependency>
        <groupId>io.teaql</groupId>
        <artifactId>teaql-spring-boot-starter</artifactId>
        <version>${teaql.version}</version>
    </dependency>
</dependencies>
```

The project was renamed from `teaql-spring-boot-starter` to `teaql-java` when it
grew from a Spring-only package into a modular Java runtime. The starter
artifact name is retained for compatibility.

### Runtime Usage

A generated TeaQL request becomes executable after its purpose is declared:

```java
SmartList<Task> tasks = Q.tasks()
    .comment("Load tasks")
    .purpose("Display the kanban board")
    .executeForList(userContext);
```

Mutations declare an audit action:

```java
task.auditAs("Move task to Done").save(userContext);
```

Applications can replace runtime services such as `RequestPolicy`,
`RuntimeLogSink`, `DataServiceRegistry`, `InternalIdGenerationService`, and
`EntityMetaFactory` in their integration layer.

## Choose Modules

Most applications need the core runtime, one data-access path, and one database
dialect. Add optional integrations only when the application uses them.

| Application | Typical modules |
| --- | --- |
| Spring Boot with JDBC | Compatibility starter, Spring JDBC provider, and one database dialect |
| Plain JVM, Quarkus, or Micronaut with JDBC | `teaql-runtime`, `teaql-data-service-sql`, `teaql-provider-jdbc`, and one database dialect |
| Android or a portable SQL client | `teaql-android` or `teaql-sql-portable`, plus a platform-specific `TeaQLDatabase` implementation |
| In-memory execution | `teaql-runtime` |

### Runtime and API

| Module | Purpose |
| --- | --- |
| `teaql-core` | Entities, requests, criteria, metadata, policies, audit contracts, and runtime interfaces |
| `teaql-runtime` | Default runtime and concurrent in-memory execution service |
| `teaql-jackson` | Explicit TeaQL entity serialization and deserialization |
| `teaql-query-json` | JSON-to-request query parsing |
| `teaql-runtime-log` | Optional file/stdout runtime logging backend |

### SQL and Providers

| Module | Purpose |
| --- | --- |
| `teaql-data-service-sql` | SQL data-service executor and adapter contracts |
| `teaql-provider-jdbc` | Direct JDBC execution adapter |
| `teaql-provider-spring-jdbc` | Spring JDBC execution adapter |
| `teaql-sql-portable` | SQL repository path through the `TeaQLDatabase` abstraction, without `spring-jdbc` |

Supported dialect modules are `teaql-sqlite`, `teaql-mysql`, `teaql-postgres`,
`teaql-oracle`, `teaql-db2`, `teaql-mssql`, `teaql-hana`, `teaql-duckdb`,
`teaql-snowflake`, and `teaql-dm8`. In normal applications, select only the
dialect for the target database.

### Optional Capabilities and Utilities

| Module | Purpose |
| --- | --- |
| `teaql-dynamic-fields-api` | Dynamic-field API and in-memory implementation |
| `teaql-dynamic-fields-jdbc` | JDBC persistence for dynamic-field definitions and values |
| `teaql-business-id-jdbc` | JDBC-backed business ID generation |
| `teaql-context-runtime-tools` | Runtime tool registration and policy integration |
| `teaql-tool-http` | Auditable HTTP tool capability |
| `teaql-android` | Android integration helpers |
| `teaql-utils`, `teaql-utils-json` | Framework-neutral utility abstractions |
| `teaql-utils-reflection`, `teaql-utils-spring` | Optional reflection- and Spring-backed utility implementations |

## Framework Notes

### Spring Boot and SQLite

`teaql-autoconfigure` provides the default Spring Boot runtime beans, while the
compatibility starter pulls that auto-configuration into an application.

SQLite applications can use standard Spring datasource properties:

```properties
spring.datasource.url=jdbc:sqlite:./data/app.db
spring.datasource.driver-class-name=org.sqlite.JDBC
```

### Android

`teaql-sql-portable` keeps `spring-jdbc` out of the repository path. Android
applications provide an Android-backed `TeaQLDatabase` implementation, and
TeaQL executes positional SQL through that abstraction. See the
[Android integration guide](teaql-sql-portable/ANDROID_GUIDE.md).

## Native Image and Reflection

The main entity construction, JSON, and SQL row-mapping paths are designed to
avoid reflection-heavy bean mutation:

- `teaql-jackson` registers explicit entity serializers and deserializers
  through `TeaQLModule`.
- `teaql-sql-portable` creates entities through
  `EntityDescriptor.createEntity()`.
- Generated or hand-written metadata registers an `entitySupplier`, such as
  `Task::new`, beside its `targetType`.

Dynamic and additional values should remain JSON-friendly: scalars, maps,
lists, or other explicitly serializable values. Arbitrary application objects
may still trigger Jackson's default bean introspection.

See the [Native Image Reflection Guide](NATIVE_IMAGE_REFLECTION_GUIDE.md) for
the baseline and coding rules.

## Build and Test

```bash
git clone https://github.com/teaql/teaql-java.git
cd teaql-java
mvn clean install
```

Useful verification commands:

```bash
mvn test
mvn spotbugs:check
```

## Documentation

- [Runtime design](RUNTIME_DESIGN.md)
- [Runtime logging design](LOG_DESIGN.md)
- [Native image reflection guide](NATIVE_IMAGE_REFLECTION_GUIDE.md)
- [Database dialect integration guide](DIALECT_INTEGRATION_GUIDE.md)
- [Dynamic Fields API](teaql-dynamic-fields-api/README.md)
- [Dynamic Fields JDBC](teaql-dynamic-fields-jdbc/README.md)
- [Changelog](CHANGELOG.md)

## Contributing and Support

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution requirements and
[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for community guidelines.

Report defects and enhancement requests through
[GitHub Issues](https://github.com/teaql/teaql-java/issues). Include the TeaQL
version, Java version, framework and database details, and reproduction steps.
For vulnerabilities, follow the private reporting process in
[SECURITY.md](SECURITY.md).

TeaQL Java is licensed under the [Apache License 2.0](LICENSE).
