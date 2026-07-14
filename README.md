# teaql-java

**TeaQL is an AI-native runtime designed for Coding Agents and modern application development.** 

While traditional frameworks assume a human is writing every line of code, TeaQL provides a strict, typed, and auditable Capability Sandbox tailored specifically for autonomous AI Agents (and humans) to execute code securely.

### The Five Safeguards of AI Coding

To ensure absolute safety and governance when AI Agents interact with production systems, the TeaQL runtime enforces the following five safeguards:

1. **Mandatory Identity (UserContext):** Every operation must pass through a runtime `UserContext`. The system explicitly records whether the action was performed by a human or an AI Agent.
2. **Intent Auditing (Typestate/Builder):** Agents cannot simply call `.execute()`. They are forced by the compiler to declare their intent using `.purpose()` (for reads) or `.auditAs()` (for writes) before the execution terminal is unlocked.
3. **Capability Sandbox (SPI/Features):** Dangerous operations (HTTP, File IO, Message Queues) are physically isolated. Unless explicitly granted in the project dependencies (via JPMS SPI or Cargo features), the Agent is structurally blocked from accessing them.
4. **Graph Mutability Control:** Agents do not manually assemble SQL `UPDATE`s or relationship loops. They operate on typed Entity Graphs (`saveGraph`), reducing hallucination-induced data corruption.
5. **Universal Error Translation:** The runtime intercepts infrastructure errors and translates them into semantic business codes, preventing the Agent from getting stuck in stack trace loops.

---

TeaQL Java is the Java runtime for TeaQL domain applications. It provides the
core entity/request/repository model, SQL repository support, database-specific
dialects, and integration modules for Spring Boot and Android.

TeaQL Java is positioned as a portable domain runtime across Android,
desktop/console, and server-side Java frameworks, with pluggable SQL/database
modules.

The main runtime path is designed to run without reflection-heavy entity
construction or bean mutation. See
[Native Image Reflection Guide](NATIVE_IMAGE_REFLECTION_GUIDE.md) for the
no-reflection baseline and coding rules.

For TeaQL entities, JSON serialization/deserialization and SQL result-set row
mapping are on that no-reflection path:

- `teaql-jackson` registers explicit TeaQL entity serializers and
  deserializers through `TeaQLModule`.
- `teaql-sql-portable` creates entities from database rows through
  `EntityDescriptor.createEntity()`, backed by registered suppliers such as
  `Task::new`.
- Generated or hand-written metadata must register `entitySupplier` beside
  `targetType`.

Keep dynamic/additional values JSON-friendly, such as scalars, maps, lists, and
other already-serializable values. If an application stores arbitrary Java
objects in dynamic/additional fields, Jackson may still use its default bean
introspection for those application objects.

The project was renamed from `teaql-spring-boot-starter` to `teaql-java` as the
runtime moved from a Spring-only package to a modular Java runtime. The Spring
Boot starter artifact remains `teaql-spring-boot-starter` for compatibility.

## Modules

### Core Modules
| Module | Purpose |
| --- | --- |
| `teaql-core` | Core entities, requests, criteria, metadata, audit logging, policies, and contracts. Completely independent of Spring/SQL. |
| `teaql-runtime` | Default runtime implementation including `TeaQLRuntime` engine, user contexts, registry lookup, and an optimized concurrent in-memory database execution service with LRU eviction. |

### Optional Modules
| Module | Purpose |
| --- | --- |
| `teaql-jackson` | Explicit TeaQL entity JSON serialization and deserialization support. |
| `teaql-query-json` | JSON query parsing support. |
| `teaql-runtime-log` | Optional runtime log sinks. |
| `teaql-context-runtime-tools` | Runtime tool registration and policy integration. |
| `teaql-tool-http` | Optional HTTP tool support. |
| `teaql-sql-portable` | Portable SQL repository through the `TeaQLDatabase` abstraction. |
| `teaql-data-service-sql` | SQL data service integration. |
| `teaql-provider-jdbc`, `teaql-provider-spring-jdbc` | JDBC provider integrations. |
| `teaql-sqlite`, `teaql-mysql`, `teaql-postgres`, etc. | Database-specific SQL repository modules. |
| `teaql-android` | Android-facing integration helpers. |

### Module Selection Matrix

`✅` means the module is directly useful for that application type. Database
modules are selected by the target database; most applications only need one
dialect module.

| Module | Android APP | Compose APP | Console APP | Spring Boot APP | Quarkus APP | Micronaut APP |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `teaql-core` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-runtime` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-jackson` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-query-json` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-runtime-log` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-sql-portable` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-data-service-sql` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-provider-jdbc` | ✅ | ✅ | ✅ |  | ✅ | ✅ |
| `teaql-provider-spring-jdbc` |  |  |  | ✅ |  |  |
| `teaql-utils` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-utils-json` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-utils-reflection` |  |  |  | ✅ |  |  |
| `teaql-utils-spring` |  |  |  | ✅ |  |  |
| `teaql-context-runtime-tools` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-tool-http` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-android` | ✅ |  |  |  |  |  |
| `teaql-sqlite` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-mysql` |  | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-postgres` |  | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-oracle` |  | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-db2` |  | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-mssql` |  | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-hana` |  | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-duckdb` |  | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-snowflake` |  | ✅ | ✅ | ✅ | ✅ | ✅ |
| `teaql-dm8` |  | ✅ | ✅ | ✅ | ✅ | ✅ |

## Requirements

- Java 17+
- Maven 3.8+

## Dependency Examples

Spring Boot applications should depend on the starter artifact:

```xml
<dependency>
    <groupId>io.teaql</groupId>
    <artifactId>teaql-spring-boot-starter</artifactId>
    <version>1.516-RELEASE</version>
</dependency>
```

Android applications should use `teaql-sql-portable` and provide a platform-specific
`TeaQLDatabase` implementation.

## Runtime Model

TeaQL request objects are executable only after the request carries enough
intent for policy and audit checks. A typical generated request flow looks like:

```java
Q.tasks()
    .comment("Load tasks")
    .purpose("Display kanban board")
    .executeForList(userContext);
```

The default runtime keeps a small execution surface. Applications can replace
`RequestPolicy`, `RuntimeLogSink`, `DataStore`, `LockService`, `Translator`, and
`EntityMetaFactory` beans in their framework integration layer. File/stdout log
writing is provided by the optional `teaql-runtime-log` module.

## Framework Integration

### Spring Boot

`teaql-autoconfigure` provides the default Spring Boot runtime beans, while the
starter artifact pulls the auto-configuration into an application.

SQLite applications can use normal Spring datasource properties:

```properties
spring.datasource.url=jdbc:sqlite:./data/app.db
spring.datasource.driver-class-name=org.sqlite.JDBC
```

When the SQLite module sees a `jdbc:sqlite:` URL, it wraps the datasource with a
single-connection datasource to avoid common SQLite multi-connection lock
contention.

### Android

`teaql-sql-portable` removes the `spring-jdbc` dependency from the repository
path. The module is not an Android SDK module, but its main use case today is
Android: application code supplies an Android-backed `TeaQLDatabase`
implementation, and the repository executes positional SQL through that
abstraction.

## Build & Development

To build the project from source, you need Java 17+ and Maven 3.8+:

```bash
git clone https://github.com/teaql/teaql-java.git
cd teaql-java
mvn clean install
```

Run tests where present:

```bash
mvn test
```

## Reporting Bugs

If you find a bug, please create an issue on [GitHub Issues](https://github.com/teaql/teaql-java/issues).
Please include as much detail as possible, such as TeaQL version, Java version, framework details, and steps to reproduce.
For security vulnerabilities, please see our [Security Policy](SECURITY.md).

Scan for Chinese comments or strings:

```bash
node scan-chinese.js
```
