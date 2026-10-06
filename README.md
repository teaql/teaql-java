# TeaQL Java

## Sensitive log data

Runtime diagnostic logs redact payload values by default, before delivery to
file, console, buffers, or custom logging sinks. Selecting a diagnostic sink
alone does not authorize plaintext. For controlled troubleshooting only:

```bash
export TEAQL_ALLOW_SENSITIVE_PLAINTEXT_LOGS=I_UNDERSTAND_SENSITIVE_DATA_MAY_BE_WRITTEN_TO_DISK
```

Only this exact value enables plaintext permission; empty values, `true`, and
whitespace variants do not. Enabling it emits a warning. Credential-classified
fields remain redacted. The flag does not force every sink to expose values.
SQL without reliable field/literal provenance may be suppressed and marked
`NOT REPLAYABLE`. Execution parameters and persisted business data are unchanged.

Do not put sensitive data in free-text comments or purpose declarations.
TeaQL cannot govern arbitrary application prints or independent driver loggers;
configure those separately. This setting does not erase older plaintext files.
Restrict access and retention when using plaintext diagnostics, then unset the
variable and restart processes when troubleshooting is complete.

[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/13612/badge)](https://www.bestpractices.dev/projects/13612)

TeaQL Java is the Java runtime for TeaQL domain applications. It provides a
typed entity and request model, auditable execution, pluggable runtime
capabilities, portable SQL support, database dialects, and integrations for
server-side Java and Android.

TeaQL is designed for applications in which code may be written or operated by
both humans and coding agents. Instead of exposing unrestricted infrastructure
operations, the runtime keeps execution behind explicit context, intent, policy,
and capability boundaries.

## Recommended Agent Harness

When building database-backed applications with the TeaQL Java runtime, we
recommend using it together with the [TeaQL Agent Kit](https://github.com/teaql/teaql-agent-kit).
The Agent Kit is TeaQL's continuously evolving **Harness Engineering** method.
It gives coding agents a model-mediated, executable workflow for domain
modeling, deterministic evaluation and repair, code generation, implementation,
and evidence-based verification as the generator and runtimes evolve.

## Why TeaQL?

TeaQL applies five safeguards to application operations:

1. **Context-bound execution** — reads and writes run through a `UserContext`,
   which carries identity, trace, and runtime capabilities.
2. **Declared intent** — reads require `.comment(...).purpose(...)`; writes use
   `.auditAs(...)` before an execution terminal becomes available.
3. **Policy gates** — `QueryPolicy` reviews reads; `MutationPolicy` reviews one
   immutable graph-level `MutationPlan` before the first provider write.
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
    <teaql.version>1.526-RELEASE</teaql.version>
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

### Request owned intent on the development branch

The `feature/request-trace-chain` branch makes non-blank `comment` part of both
`QueryRequest` and `MutationRequest`. Query also requires non-blank `purpose`.
Existing generated `.comment(...).purpose(...)` and `.auditAs(...)` spelling
does not change. Low-level provider requests now expose an immutable validated
`QueryIntent` or `MutationIntent`; custom SPI implementations must adopt this
contract. These changes are not a claim about published Maven artifacts.

Missing or Unicode-whitespace-only comment fails with
`REQUEST_COMMENT_REQUIRED` at `comment`; missing Query purpose fails with
`QUERY_PURPOSE_REQUIRED` at `purpose`. Validation happens before policy and
provider execution, including direct runtime calls and disabled logging.
Neither a Context default nor a fabricated trace supplies missing intent.

List, aggregate, relation and streaming execution no longer push or pop query
frames on Context. Streaming providers now accept the same validated
`QueryRequest` envelope as materialized providers instead of a bare
`SearchRequest`. A custom `StreamingQueryExecutor` must migrate that SPI
signature; generated `.executeForStream(context)` calls remain unchanged.
The captured intent survives Policy changes to a builder and delayed cursor
consumption. SQL providers snapshot source paths and redaction provenance for
the invocation, including inherited internal streams. Legacy unbound direct SQL
diagnostics can still use explicitly supplied Context frames; those compatibility
calls are not the runtime query ownership contract.

Derived relation, Facet and materialized relation-predicate queries carry their
validated originating intent instead of asking callers to repeat it. Mutation
reason is captured before policy and retained in provider requests and committed
audit facts. Graph saves now use immutable parent-linked mutation scopes, rather
than a Context push/pop stack. Each persistence request carries its own typed
lineage through SQL writes, authoritative readback and committed safe audit.
`TraceNode` includes entity type and assigned ID; Entity and Ledger trace setters
now accept immutable `List<TraceNode>`, not flattened strings. Custom callers
using the old string API must migrate; this is a local source change, not a
released API. SQL `tracePath` remains separate from `mutationLineage`.

The local runtime tests cover branch reasons, deleted children, same numeric ID
across types, assigned IDs, complete ledger overrides and concurrent saves sharing
one Context. Actual SQLite tests cover SQL/audit propagation, provider rollback,
readback failure/retry and masking. Native batch diagnostics distinguish the
batch call's `batchOutcome` from an individual member's possibly unknown
`executionOutcome`. Same-type graph inserts now use a validated
`MutationBatchRequest` and the optional `BatchMutationExecutor` capability.
Physical JDBC rows retain separate immutable trace bindings, including failure
and readback diagnostics; incompatible insert column layouts are grouped separately.
Root intent remains required even if children are annotated or logs are disabled.
Providers without the capability retain individual command execution.

Internal reverse-list attachment-key projection preserves an explicitly requested
nested forward load. It must not use the public scalar selection operation that
removes a same-named relation load. Native SQLite tests cover root/nested graphs,
window/probe plans and logging on/off; the example gate runs these tests too.
Java retains an ID-only reference when its forward query has no matching target:
non-loaded fields remain guarded by `TeaQLNotLoadedException`, while list
membership and independent counts survive. This is not a claim of null-valued
reference parity with other runtimes.

Bootstrap follows the same request and audit boundary. Call
`context.ensureSchema()` with the generated Runtime Module installed: providers
perform physical DDL, then generated Q and audited Mutation reconcile roots and
constants. The legacy Portable `ensureSchema(context, type)` and repository
`ensureInitData(context)` data-write APIs have been removed. The
[School example](examples/school-management/README.md) verifies real bootstrap
SQL intent, committed lineage, fixed IDs, no-op reseeding and versioned constant
reconciliation on two starts of the same database.

The generated [Trace Chain example](examples/trace-chain/README.md) proves the
normative graph, overlapping three-level Q/E queries, late-consumed streams,
nested Facets with the original root and complete relation paths,
prepared insert grouping and complete ledger replacement, plus prepared
update/delete/recover batches with independent
optimistic versions. It now also runs real overlapping generated Checkers and
independent graph saves with one Context, observing per-item SQL and committed
audit lineage. Temporary check results, visited objects, Fix evidence and the
captured graph clock belong to each synchronous Checker invocation. Nested saves
restore the outer invocation while preserving the original custom Context and
its service hooks. `lastFixEvidence()` is the last completed check's diagnostic
receipt on the calling execution thread; it is not an async propagation API.
Read-only loaded relations retain their private ledger when reused by independent
graphs. Graph composition imports only pending mutations of explicitly visited
related entity keys, not every pending key from a foreign reference's ledger.
The provider-route guard also belongs to each mutation plan, not a retained
Context attribute. Independent graphs may use different providers on one
Context. A single atomic graph with writes to different routes is rejected
before mutation execution; read-only references do not count as writes.
Native tests cover separate SQLite databases and actual overlapping threads.

Native dynamic-aggregation tests also retain the original root through nested
relations, preserve inherited masking provenance and avoid fabricated relation
nodes for numeric partitions. They are separate from generated Facet acceptance.

Complete entry-point/privacy coverage, legacy unbound SQL diagnostic migration, asynchronous
handoff/cancellation and immutable internal Registry replay remain separate open
gates. The tested SQLite writer transactions serialize while the generated
Checkers overlap. This is local source evidence, not a merge or release claim.

Applications can replace runtime services such as `QueryPolicy`, the
`MutationPolicyRegistry`, `MutationPolicyApprovalProvider`, `RuntimeLogSink`,
`DataServiceRegistry`, `InternalIdGenerationService`, and `EntityMetaFactory`
in their integration layer.

Query and Mutation execution logs are enabled by default. The built-in default
sink is safe for ordinary operator output: it includes intent, trace, elapsed
time, outcome, and SQL with safely rendered parameters. Sensitive values follow
the field's masking policy; an unsafe statement is omitted with a reason rather
than printed as plaintext. Select an additional diagnostic destination only for
controlled troubleshooting:

```java
TeaQLRuntime runtime = TeaQLRuntime.builder()
    .metadata(metadata)
    .queryExecutionLogging(true)
    .mutationExecutionLogging(true)
    .diagnosticSqlLogging(true) // selecting a destination alone does not authorize plaintext
    .build();
```

The Query and Mutation switches remain independent. Selecting diagnostic SQL
changes the built-in destination; it does not enable or disable either family.
Custom `RuntimeLogSink` implementations receive safe SQL projections too.
Plaintext requires both an explicitly sensitive destination and the exact
`TEAQL_ALLOW_SENSITIVE_PLAINTEXT_LOGS=I_UNDERSTAND_SENSITIVE_DATA_MAY_BE_WRITTEN_TO_DISK`
acknowledgement. Debug records are individually labeled; credentials remain protected.
Custom `UserContext` implementations must also explicitly delegate or override
`requiresSensitiveSqlLogData()` when they enable a diagnostic sink.
The optional file-backed `LogManager` requests value-bearing SQL only with
`TEAQL_SQL_LOG=_full_with_payload`; restrict access and retention before enabling it.

## Security Foundations

TeaQL Java is a server-side security reference runtime:

- ordinary Query and Mutation logs are enabled by default and retain intent,
  typed trace, safely expanded SQL, timing, and outcome;
- ordinary SQL remains copy/paste-readable with masked values; plaintext requires
  a sensitive diagnostic sink and the exact environment acknowledgement;
- the TFP endpoint applies trusted server policy, bounded queries, writable-field
  rules, tenant scope, and optimistic version in the provider operation;
- boundary-facing entity references can be issued and verified through
  `UserContext` without serializing raw internal ID/version pairs.

Install an application-owned key provider at the runtime boundary:

```java
byte[] activeKey = loadThirtyTwoByteKeyFromSecretManager();
var provider = AeadRoundTripReferenceProvider.fromProcessEnvironment(
    DeploymentProfile.PRODUCTION,
    "order-service",
    "production",
    new StaticRoundTripReferenceKeyProvider(
        new RoundTripReferenceKey("k2", activeKey)),
    currentAuthorizationPolicy);
var runtime = TeaQLRuntime.builder()
    .roundTripReferenceProvider(provider)
    .build();
userContext.withTrustedReferencePrincipal(
    new TrustedReferencePrincipal("oidc", "alice", "Platform", 7));
var scope = new ReferenceDocumentScope(
    "order-editor-100", "edit-order", "Order", 100, 9);

Object wire = userContext.referenceFor(orderItem, scope, Duration.ofMinutes(15));
ResolvedRoundTripReference resolved = userContext.resolveReference(
    wire, scope, "OrderItem");
```

The `tqr1` token uses AES-256-GCM with HKDF-SHA-256, carries a key ID for
rotation, expires, and binds Actor, Domain Root, service, environment, purpose,
document, Aggregate identity/revision, entity type/ID/version. Current
authorization runs both when issuing and consuming the reference. Java and
Rust retain the same deterministic golden vector.

For local diagnosis only, the exact
`TEAQL_UNSAFE_EXPOSE_RAW_ENTITY_IDS=I_UNDERSTAND_THIS_EXPOSES_INTERNAL_ENTITY_IDS_FOR_LOCAL_DEBUGGING_ONLY`
acknowledgement changes the startup-selected wire shape to `{id, version}` in
development/test. Production fails closed; raw mode does not bypass current
authorization, type, version, projection, Checker/Fix, audit, or Mutation
Ledger enforcement. See the canonical
[context-bound reference contract](https://github.com/teaql/teaql-conformance/blob/main/design/context-bound-round-trip-references.md).

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

For business IDs, use the context-owned `BusinessIdService` and explicit
`context.ensureSchema()` lifecycle. The [focused runtime example](examples/business-id-runtime/README.md)
documents the preferred contract and the deprecated legacy boundary.

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

The PostgreSQL/MySQL integration tests are optional during an ordinary local
build, but the `Live SQL dialects` CI workflow requires both databases and
fails instead of counting a skipped connection as a pass. Run them locally
against dedicated, freshly created `teaql_live_*` databases (never a shared
application database):

```bash
export TEAQL_REQUIRE_LIVE_DB=true
export TEAQL_TEST_POSTGRES_URL=jdbc:postgresql://127.0.0.1:5432/teaql_live_local
export TEAQL_TEST_POSTGRES_USER=<test-user>
export TEAQL_TEST_POSTGRES_PASSWORD=<test-password>
export TEAQL_TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:3306/teaql_live_local?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true'
export TEAQL_TEST_MYSQL_USER=<test-user>
export TEAQL_TEST_MYSQL_PASSWORD=<test-password>
mvn -pl teaql-postgres,teaql-mysql -am \
  -Dtest=PostgresIntegrationTest,MysqlIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

The tests create `task_data`, `context_probe_data`, and `teaql_id_space` within
those isolated databases. In addition to schema and CRUD behavior, each dialect
uses the production `IdSpaceIdGenerator` for entity saves and verifies 40
contended allocations across four independent generator instances followed by
restart continuity. Dispose of the databases after the run.

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
