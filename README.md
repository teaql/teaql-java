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

The project was renamed from `teaql-spring-boot-starter` to `teaql-java` as the
runtime moved from a Spring-only package to a modular Java runtime. The Spring
Boot starter artifact remains `teaql-spring-boot-starter` for compatibility.

## Modules

### Core Modules (Active)
| Module | Purpose |
| --- | --- |
| `teaql-core` | Core entities, requests, criteria, metadata, audit logging, policies, and contracts. Completely independent of Spring/SQL. |
| `teaql-runtime` | Default runtime implementation including `TeaQLRuntime` engine, user contexts, registry lookup, and an optimized concurrent in-memory database execution service with LRU eviction. |

### Reference Modules (In `reference/` directory, for progressive transformation)
| Module | Purpose |
| --- | --- |
| `teaql-utils` | Shared utility classes used by the runtime (now with optimized reflection cache). |
| `teaql-sql` | SQL repository implementation based on `spring-jdbc`. |
| `teaql-autoconfigure` | Spring Boot auto-configuration for TeaQL runtime beans. |
| `teaql-starter` | Compatibility starter artifact `teaql-spring-boot-starter`. |
| `teaql-sql-portable` | Portable SQL repository through the `TeaQLDatabase` abstraction (e.g. for Android). |
| `teaql-sqlite` | SQLite repository support. |
| `teaql-mysql`, `teaql-mssql`, `teaql-oracle`, etc. | Database-specific SQL repository modules. |

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

The default `PurposeRequestPolicy` enforces this style by requiring a purpose
before execution. Applications can replace `RequestPolicy`, `LogManager`,
`DataStore`, `LockService`, `Translator`, and `EntityMetaFactory` beans in their
framework integration layer.

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

## Development

Compile all modules:

```bash
mvn clean compile
```

Run tests where present:

```bash
mvn test
```

Scan for Chinese comments or strings:

```bash
node scan-chinese.js
```
