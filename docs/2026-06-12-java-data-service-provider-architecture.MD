# Java Data Service Provider Architecture

Date: 2026-06-12

## Conclusion

TeaQL Java should align conceptually with the Rust `teaql-data-service` model.

The Java runtime should not be centered on Spring beans, Spring annotations, JDBC, or SQL repositories. It should be centered on `UserContext`, with query, aggregation, mutation, schema, and transaction operations routed through a Data Service provider architecture.

`teaql-core` should be the contract center: it defines the TeaQL world model and the contracts for operating on that world. `teaql-runtime` should provide the basic default runtime capabilities that implement those contracts.

Spring, JDBC, Android SQLite, memory, Meilisearch, and future remote/search/vector backends are providers, execution adapters, or backend resources. They are not the runtime center.

## Problem

The current Java runtime still reflects its Spring Boot origin:

- `TQLResolver` uses a bean-container shape: `getBean`, `getBeans`, `getBean(String)`.
- `GLobalResolver` provides a static runtime fallback.
- `teaql-sql` combines SQL compilation, repository behavior, Spring JDBC execution, and Spring transaction support.
- `teaql-sql-portable` is intended to support Android-style runtimes, but it still depends on `teaql-sql`, which depends on Spring JDBC.
- Non-Spring environments are currently framed as "use JDBC directly", which is too narrow.

Rust already has a broader abstraction: `teaql-data-service`. It can support SQL providers, memory providers, and providers like Meilisearch. Java should follow that direction.

## Design Principle

The Java runtime should be `UserContext`-centered and provider-oriented:

```text
Generated Q / Entity / Request API
        |
        v
UserContext
        |
        v
DataServiceRegistry
        |
        v
DataServiceExecutor
        |
        +-- SQL provider
        +-- Memory provider
        +-- Meilisearch provider
        +-- Remote provider
        +-- Future providers
```

SQL is one provider, not the runtime model.

JDBC is one SQL execution adapter, not the portable runtime model.

Spring is one framework adapter, not the runtime model.

## Core and Runtime

TeaQL Java should separate the contract center from the default runtime implementation.

```text
teaql-core
  defines the TeaQL world and the contracts for operating on that world

teaql-runtime
  provides the basic default way to operate that world
```

`teaql-core` is not a utility module. It is the contract center. It should contain stable world-model concepts and runtime contracts.

Core world-model concepts:

```text
Bean
  a TeaQL-described object unit; this is a TeaQL concept, not a Spring bean

Entity
  a persistable, queryable, mutable, relatable Bean

EntityProperty
  the structural unit used to describe an Entity property

Relation
  an EntityProperty that connects one Entity to another Entity or Entity collection

Metadata
  EntityDescriptor, PropertyDescriptor, Relation, EntityMetaFactory,
  PropertyType, and related descriptor contracts
```

Core request-model concepts:

```text
SearchRequest
MutationRequest
AggregationRequest
Criteria
Expression
Projection
OrderBy
Slice
FacetRequest
```

Core runtime contracts:

```text
UserContext
DataServiceRegistry
DataServiceExecutor
QueryExecutor
MutationExecutor
TransactionExecutor
SchemaExecutor
DataServiceCapabilities
ExecutionMetadata
RequestPolicy
InternalIdGenerationService
RuntimeLogSink
TransactionCallback
```

`UserContext` is the central core object. Generated code should enter TeaQL through `UserContext`; environment integration should construct or adapt `UserContext`; backend access should route from `UserContext` to Data Services.

`teaql-runtime` provides the basic implementation of the core contracts:

```text
DefaultUserContext
TeaQLRuntime
TeaQLRuntimeBuilder
DefaultDataServiceRegistry
Default request execution orchestration
Default request policy handling
Default internal Long ID generation
Default execution logging
Default transaction coordination
Runtime-scoped attributes
Legacy compatibility adapters
```

`teaql-runtime` must not bind to Spring, JDBC, SQL dialects, Android SQLite, GraphQL, or web rendering. Those belong to provider, adapter, or integration modules.

The distinction is:

```text
teaql-core explains what TeaQL is.
teaql-runtime provides the default way to run TeaQL.
```

## teaql-core Class Boundary

`teaql-core` may contain stable concepts, request and metadata models, `UserContext`-centered runtime contracts, and tiny convenience methods that delegate to those contracts. It must not contain environment binding, backend implementation, repository implementation, SQL/JDBC/Spring/Web/GraphQL logic, or default runtime behavior.

### World Model

These classes and concepts define the TeaQL world and belong in `teaql-core`:

```text
Bean
Entity
BaseEntity
EntityStatus
EntityAction
EntityActionException
ConcurrentModifyException
```

Metadata classes and concepts:

```text
EntityDescriptor
EntityMetaFactory
PropertyDescriptor
EntityProperty
PropertyType
Relation
MetaConstants
```

They belong in core because they define what exists in the TeaQL world.

### Request Model

These classes express operations against the TeaQL world and belong in `teaql-core`:

```text
SearchRequest
BaseRequest
ExecutableRequest
FacetRequest
AggregationItem
AggregationResult
Aggregations
SimpleAggregation
Slice
OrderBy
OrderBys
```

Criteria and expression classes also belong in core when they are provider-neutral:

```text
SearchCriteria
TypeCriteria
SubQuerySearchCriteria
Expression
PropertyAware
PropertyReference
PropertyFunction
FunctionApply
Constant
Parameter
AggrExpression
AggrFunction
SimpleNamedExpression
```

Provider-neutral criteria implementations:

```text
AND
OR
NOT
EQ
GT
GTE
LT
LTE
IN
NotIn
Between
Contain
NotContain
BeginWith
EndWith
IsNull
IsNotNull
VersionSearchCriteria
```

SQL-specific request constructs should be moved out of core over time:

```text
RawSql
```

`RawSql` leaks SQL into the provider-neutral request model. It should become a SQL Provider extension or be represented through a provider-specific extension mechanism.

### SearchRequest Boundary

`SearchRequest` is the provider-neutral read intent model of TeaQL.

It may expose execution convenience methods in `teaql-core`:

```text
SearchRequest.executeForList(ctx)
SearchRequest.executeForOne(ctx)
SearchRequest.executeForStream(ctx)
SearchRequest.aggregation(ctx)
ExecutableRequest.executeForList(ctx)
ExecutableRequest.executeForOne(ctx)
ExecutableRequest.executeForStream(ctx)
ExecutableRequest.aggregation(ctx)
```

These methods must remain very small. They are allowed only as core-level API ergonomics and must delegate to `UserContext`.

Allowed shape:

```java
default SmartList<T> executeForList(UserContext ctx) {
    if (ctx == null) {
        throw new TQLException("UserContext is required");
    }
    return ctx.executeForList(this);
}
```

Not allowed in `SearchRequest`, `BaseRequest`, or `ExecutableRequest`:

```text
Repository lookup
Data Service selection
Provider selection
SQL compilation
Transaction management
Relation loading implementation
Execution logging implementation
Policy enforcement implementation
Backend-specific planning
```

The real execution path must remain:

```text
SearchRequest.executeForList(ctx)
  -> UserContext.executeForList(request)
    -> TeaQLRuntime
      -> DataServiceRegistry
        -> DataServiceExecutor
          -> Provider
            -> Execution Adapter
              -> Backend Resource
```

This keeps the generated API ergonomic without moving execution behavior into the request model.

### Runtime Contracts

These interfaces and contracts belong in `teaql-core`:

```text
UserContext
RequestPolicy
InternalIdGenerationService
DataServiceRegistry
DataServiceExecutor
QueryExecutor
MutationExecutor
TransactionExecutor
SchemaExecutor
DataServiceCapabilities
ExecutionMetadata
TransactionCallback
RuntimeLogSink
```

The contract belongs in core; concrete implementations belong in `teaql-runtime`, provider modules, adapter modules, or integration modules.

For internal ID generation, the core contract is:

```text
InternalIdGenerationService
```

Concrete internal Long ID strategies belong in `teaql-runtime` or a dedicated implementation module, not in `teaql-core`.

### Base Results and Exceptions

These base result and exception concepts belong in `teaql-core`:

```text
TQLException
RepositoryException
DataServiceException
SmartList
QueryResult
MutationResult
AggregationResult
```

`RepositoryException` may remain in core for compatibility. Conceptually, future Data Service code should prefer `TQLException` or `DataServiceException`.

### Not Core

These classes or concepts should not be part of `teaql-core`:

```text
Repository
AbstractRepository
RepositoryAdaptor
TeaQLDatabase
TQLResolver
GLobalResolver
DataStore
GraphQLService
BaseService
DynamicSearchHelper / JsonRequests (`teaql-query-json`)
PurposeRequestPolicy implementation
SimpleLockService
LogManager implementation (`teaql-runtime-log`)
SqlLogEntry
WebResponse
ViewRender
ServiceRequestUtil
UserContextInitializer
Language translator implementations
BaseInternalRemoteIdGenerator
```

Some of them may depend on `teaql-core`, but they are runtime, provider, adapter, integration, or compatibility concerns.

## Bottom Layer Model

The bottom layers should use four distinct concepts:

```text
Data Service
  -> Provider
    -> Execution Adapter
      -> Backend Resource
```

### Data Service

Data Service is the TeaQL semantic boundary visible to the runtime.

It accepts and returns TeaQL concepts:

```text
QueryRequest
MutationRequest
AggregationRequest
SchemaRequest
TransactionCallback
ExecutionMetadata
DataServiceCapabilities
```

Responsibilities:

- Receive TeaQL query, mutation, aggregation, schema, and transaction requests.
- Declare capabilities such as query, mutation, transaction, full-text search, schema, and returning support.
- Act as the routing target selected from entity metadata, for example `Order -> sql` and `ProductSearch -> meilisearch`.
- Expose unified executor contracts such as `DataServiceExecutor`, `QueryExecutor`, `MutationExecutor`, and `TransactionExecutor`.

It must not expose JDBC, Spring, HTTP clients, Android SQLite APIs, or SQL dialect details.

### Provider

Provider is the semantic implementation for a backend family.

Examples:

```text
SQL Provider
Memory Provider
Meilisearch Provider
Remote API Provider
```

Responsibilities:

- Interpret TeaQL requests for a backend family.
- Compile or translate TeaQL operations into provider-native operations.
- Own provider capabilities and unsupported-operation errors.

Examples:

```text
SQL Provider
  compiles QueryRequest into SQL, bind parameters, and mapping instructions

Meilisearch Provider
  compiles SearchRequest into search payloads, filters, facets, and index operations

Memory Provider
  evaluates requests against in-memory predicates and stores
```

SQL is a Provider. Meilisearch is a Provider. Memory is a Provider. JDBC is not a Provider; it is one execution adapter for the SQL Provider.

### Execution Adapter

Execution Adapter binds a Provider to a concrete technology stack.

Examples:

```text
JdbcExecutionAdapter
SpringJdbcExecutionAdapter
AndroidSQLiteExecutionAdapter
MeilisearchHttpAdapter
InMemoryStoreAdapter
```

Responsibilities:

- Manage concrete clients, connections, sessions, or handles.
- Execute provider-native operations.
- Implement transaction mechanics when supported.
- Handle resource lifecycle, exception translation, timeout behavior, and framework integration.

Execution adapters may depend on Spring, JDBC, Android, HTTP SDKs, or local storage APIs. Those dependencies must not leak into `teaql-core`.

### Backend Resource

Backend Resource is the real external or local data system.

Examples:

```text
PostgreSQL
MySQL
SQLite
Meilisearch server
Java collections
Remote HTTP service
```

Responsibilities:

- Store or serve data.
- Execute the actual query, search, mutation, or transaction.
- Provide the real consistency and transaction guarantees, or explicitly provide none.

Backend Resource is not a TeaQL module or TeaQL contract. Only an Execution Adapter should touch it directly.

## Rust Alignment

Java should mirror the core Rust concepts:

| Rust concept | Java target concept | Purpose |
| --- | --- | --- |
| `DataServiceExecutor` | `DataServiceExecutor` | Base capability-bearing executor |
| `QueryExecutor` | `QueryExecutor` | Execute generated query requests |
| `MutationExecutor` | `MutationExecutor` | Execute insert, update, delete, recover, batch |
| `TransactionExecutor` | `TransactionExecutor` | Begin provider-owned transaction |
| `SchemaExecutor` | `SchemaExecutor` | Ensure or inspect provider schema |
| `DataServiceCapabilities` | `DataServiceCapabilities` | Declare supported backend behavior |
| `ExecutionMetadata` | `ExecutionMetadata` | Record backend, operation, timing, trace, debug query |
| `EntityDescriptor.data_service` | `EntityDescriptor.dataService` | Route entity operations to a named provider |

Java does not need to copy Rust syntax, but it should preserve the same architectural boundaries. One deliberate Java boundary is that TeaQL internal ID generation remains a separate runtime capability, not part of the Data Service executor contract.

## Core Interfaces

The top-level Data Service contracts belong in `teaql-core`.

```java
public interface DataServiceExecutor {
    String name();

    DataServiceCapabilities capabilities();
}
```

```java
public interface QueryExecutor extends DataServiceExecutor {
    QueryResult query(UserContext ctx, QueryRequest request);
}
```

```java
public interface MutationExecutor extends DataServiceExecutor {
    MutationResult mutate(UserContext ctx, MutationRequest request);
}
```

```java
public interface TransactionExecutor extends DataServiceExecutor {
    <T> T executeInTransaction(UserContext ctx, TransactionCallback<T> action);
}
```

Provider-specific implementations may implement only the interfaces they support.

For example:

- SQL provider with a JDBC execution adapter: query, mutation, aggregation, schema, transaction.
- Memory provider: query, mutation, aggregation, transaction by snapshot or no-op.
- Meilisearch provider: query/search and document mutation, but no relational transaction.
- Remote API provider: capability depends on the remote service.

## Capabilities

Capabilities must be explicit. The runtime must not assume that every provider supports SQL-style behavior.

```java
public final class DataServiceCapabilities {
    private boolean query;
    private boolean streamingQuery;
    private boolean mutation;
    private boolean batchMutation;
    private boolean aggregation;
    private boolean transaction;
    private boolean schema;
    private boolean relationLoad;
    private boolean relationMutation;
    private boolean fullTextSearch;
    private boolean returning;
}
```

Before executing an operation, the runtime checks the target provider capability and fails early with a clear error when unsupported.

This matters for providers such as Meilisearch. It can search and accept document updates, but it should not be treated as a relational SQL database with joins, foreign keys, or local ACID transactions.

## Execution Metadata

SQL logging should become a specialization of Data Service execution logging.

```java
public final class ExecutionMetadata {
    private String backend;
    private DataServiceOperation operation;
    private Instant startedAt;
    private Instant endedAt;
    private Long affectedRows;
    private Integer resultCount;
    private String backendRequestId;
    private String debugQuery;
    private List<TraceNode> traceChain;
    private String comment;
}
```

Examples:

- SQL provider: `debugQuery` can be SQL with bound parameters.
- Meilisearch provider: `debugQuery` can be HTTP method, URL, and JSON payload.
- Memory provider: `debugQuery` can describe the evaluated criteria.
- Remote provider: `backendRequestId` can carry a remote request ID.

`SqlLogEntry` may remain for compatibility, but the future logging layer should center on `DataServiceExecutionLog`.

## Routing

Java metadata needs a `dataService` concept equivalent to Rust's `data_service`.

```text
Order          -> sql
ProductSearch  -> meilisearch
AuditEvent     -> sql or append-log
CacheEntry     -> memory
```

The runtime flow should be:

```text
SearchRequest
  -> UserContext
  -> resolve EntityDescriptor
  -> read descriptor.dataService, defaulting to "sql"
  -> DataServiceRegistry.resolve(dataService)
  -> execute through the matching DataServiceExecutor
```

No application code should need to know whether a request is backed by Spring JDBC, plain JDBC, Android SQLite, memory, or Meilisearch.

## Internal ID Generation Boundary

Internal ID generation is not a Data Service capability.

The Data Service layer executes query, aggregation, mutation, schema, and provider-specific data operations. TeaQL internal ID generation is a separate runtime service whose job is narrow: generate the internal `Long` ID used by TeaQL entities.

The Java runtime should keep a dedicated contract:

```java
public interface InternalIdGenerationService {
    Long generateId(UserContext ctx, Entity entity);
}
```

The existing `InternalIdGenerator` can evolve into, or be adapted by, this service.

SQL providers may use database-generated values when a mutation explicitly asks for returning/generated values, but that is not the same as owning TeaQL internal ID generation. Provider returning support remains a mutation capability; internal ID generation policy remains runtime-level.

Other identifier concepts, such as common IDs, business numbers, external IDs, remote sequence tokens, or provider-native document IDs, are out of scope for this design and should be handled separately later.

The target flow is:

```text
Entity needs ID
  -> UserContext / TeaQLRuntime
  -> InternalIdGenerationService
  -> internal Long ID attached to entity or mutation command
  -> DataServiceExecutor.mutate(...)
```

This keeps TeaQL internal ID strategy independent from data storage backend selection.

## Registry

`DataServiceRegistry` is the explicit runtime entry point for provider lookup.

```java
public interface DataServiceRegistry {
    DataServiceExecutor resolve(String name);

    QueryExecutor resolveQueryExecutor(String name);

    MutationExecutor resolveMutationExecutor(String name);

    Optional<TransactionExecutor> resolveTransactionExecutor(String name);
}
```

The default non-Spring runtime should use explicit registration:

```java
TeaQLRuntime runtime = TeaQLRuntime.builder()
    .metadata(entityMetaFactory)
    .dataService("sql", sqlDataService)
    .dataService("memory", memoryDataService)
    .requestPolicy(new PurposeRequestPolicy())
    .logSink(LogManager.getInstance())
    .build();

UserContext ctx = new DefaultUserContext(runtime);
```

Spring Boot can adapt this through beans, but the explicit runtime builder remains the main architecture path.

## Transaction Model

Transactions are a Data Service capability implemented through Provider and Execution Adapter layers.

The runtime must not pretend that all providers support ACID transactions.

```text
SQL Spring JDBC
  joins Spring @Transactional or opens TransactionTemplate

SQL plain JDBC
  uses Connection, autoCommit=false, and thread-bound transaction state

Android SQLite
  uses SQLiteDatabase.beginTransaction / setTransactionSuccessful / endTransaction

Memory
  can use snapshot, copy-on-write, or no-op semantics

Meilisearch
  usually does not support local relational transactions

Remote providers
  depend on remote backend semantics
```

Transaction responsibility follows the bottom layer model:

```text
Data Service
  exposes transaction capability and transaction entry points

Provider
  decides whether the backend family has meaningful transaction semantics

Execution Adapter
  implements concrete mechanics such as Spring transaction joining,
  JDBC commit/rollback, or Android SQLite transaction calls

Backend Resource
  enforces the real guarantee, or provides no transaction guarantee
```

Default nested transaction semantics should be `REQUIRED`:

```text
If current provider transaction exists:
  reuse it
else:
  open a provider transaction
```

`REQUIRES_NEW` and cross-provider transactions are not part of the first design.

Cross-provider mutation should be treated as eventual consistency, outbox, compensation, or explicit unsupported behavior. It should not be represented as a local ACID transaction.

## SQL Provider Stack

SQL is still important, but it should be expressed through the four bottom layers:

```text
Data Service
  SqlDataServiceExecutor

Provider
  SQL query compiler
  SQL mutation compiler
  SQL schema support
  SQL mapping

Execution Adapter
  SpringJdbcSqlExecutor
  JdbcSqlExecutor
  AndroidSQLiteSqlExecutor

Backend Resource
  PostgreSQL, MySQL, SQLite, Oracle, DB2, Snowflake, DuckDB, ...
```

Execution adapters:

```text
SpringJdbcSqlExecutor
  -> DataSourceUtils
  -> TransactionTemplate
  -> Spring-managed transaction context

JdbcSqlExecutor
  -> DataSource / Connection / PreparedStatement
  -> direct JDBC transaction

AndroidSQLiteSqlExecutor
  -> SQLiteDatabase
  -> Android SQLite transaction
```

`TeaQLDatabase` should be treated as a low-level SQL execution adapter interface, or be replaced by more explicit adapter contracts. It is not the runtime-level Data Service abstraction.

## Meilisearch Provider Shape

A Java Meilisearch provider should be possible without changing generated Q APIs.

```text
Q.products()
  -> SearchRequest
  -> descriptor.dataService = "meilisearch"
  -> MeilisearchDataServiceExecutor
  -> Meilisearch Provider compiles criteria to filter/search payload
  -> Meilisearch HTTP Execution Adapter sends request
  -> Meilisearch server
  -> map hits to Records / entities
```

Expected capabilities:

```text
query: true
fullTextSearch: true
mutation: true, for document indexing/update/delete
aggregation: limited or false, depending on supported facets/stats
transaction: false
schema: limited, for index/settings sync
relationLoad: false unless explicitly implemented
relationMutation: false
```

Unsupported operations should fail with a provider-specific, AI-actionable message:

```text
Provider "meilisearch" does not support relation mutation for entity ProductSearch.
Use the SQL data service for graph mutation, or publish search documents through an outbox/indexing flow.
```

## Module Direction

Target module layout:

```text
teaql-core
  TeaQL world model and runtime contracts.
  Includes Bean, Entity, EntityProperty, Relation, metadata descriptors,
  request models, UserContext, RequestPolicy, Data Service contracts,
  InternalIdGenerationService, transaction contracts, and execution log contracts.
  No Spring, JDBC, SQL dialect, Android, GraphQL, web rendering, or concrete backend implementation.

teaql-runtime
  Basic default implementation of teaql-core contracts.
  Includes DefaultUserContext, TeaQLRuntime, runtime builder,
  DefaultDataServiceRegistry, default policy handling, default internal Long ID generation,
  default execution logging, transaction coordination, and compatibility adapters.
  No Spring, JDBC, SQL dialect, Android, GraphQL, or web rendering dependency.

teaql
  Compatibility or aggregation artifact during migration.
  It can depend on teaql-core and teaql-runtime while legacy code is moved out.

teaql-sql
  SQL Provider.
  Includes SQL compiler, SQL metadata mapping, SQL Data Service executor,
  SQL schema support, and repository compatibility facade.
  no Spring dependency

teaql-sql-portable
  Portable SQL Execution Adapter.
  Supports no-Spring SQL execution for lightweight JVM, embedded, and Android-oriented use cases.
  Should not depend on Spring JDBC.

teaql-sqlite / teaql-mysql / teaql-oracle / teaql-hana /
teaql-db2 / teaql-mssql / teaql-snowflake / teaql-duck
  SQL dialect modules.
  Handle dialect-specific SQL, type mapping, pagination, DDL, functions, and database differences.

teaql-memory
  Memory Provider.
  Implements Data Service contracts using in-memory storage and evaluation.

teaql-graphql
  GraphQL integration.
  Adapts TeaQL request and metadata concepts to GraphQL APIs.

teaql-meilisearch
  Future Meilisearch Provider.
  Implements search-oriented Data Service capabilities using Meilisearch.

teaql-autoconfigure
  Spring Boot integration.
  Wires UserContext, TeaQLRuntime, Data Services, SQL Provider,
  Spring transaction-aware execution adapters, metadata, policy, and logging.

teaql-starter
  Spring Boot starter aggregation artifact.
```

The existing `teaql` artifact can remain as a compatibility artifact during migration, but the clean target is `teaql-core` for contracts and `teaql-runtime` for the default implementation.

## Relationship to Spring

Spring remains supported, but only as an adapter.

Spring Boot responsibilities:

- Create or adapt `UserContext`.
- Create or adapt `TeaQLRuntime`.
- Discover or create Data Service executors.
- Register them into `DataServiceRegistry`.
- Attach servlet or reactive request data.
- Adapt Spring transactions for SQL execution adapters.

Spring should not define the core runtime shape.

## Relationship to Generated Code

Generated Java code should not assume Spring annotations or Spring beans.

Generator targets should be explicit:

```text
java-runtime
  entities, requests, Q, E, checkers, metadata, no Spring annotations
  depends on teaql-core

java-spring-boot
  Spring controllers, @TQLContext integration, Spring configuration
  depends on teaql-core and teaql-runtime

java-sql
  SQL metadata and wiring hints for SQL providers
```

Generated request execution remains stable:

```java
Q.orders()
    .filterByMerchant(ctx.getMerchant())
    .comment("Load merchant orders")
    .purpose("Render order list")
    .executeForList(ctx);
```

The runtime decides which Data Service handles `Order`.

## Migration Plan

1. Add `teaql-core` with world-model concepts and runtime contracts.
2. Add `teaql-runtime` with `DefaultUserContext`, `TeaQLRuntime`, runtime builder, default registry, default policy handling, default internal Long ID generation, and default execution logging.
3. Add Data Service contracts to `teaql-core`: `DataServiceExecutor`, `QueryExecutor`, `MutationExecutor`, `TransactionExecutor`, `SchemaExecutor`, `DataServiceCapabilities`, `ExecutionMetadata`, and `DataServiceRegistry`.
4. Add `dataService` to Java entity metadata, defaulting to `sql`.
5. Add an independent `InternalIdGenerationService` path and keep internal Long ID generation outside Data Service capabilities.
6. Route query, aggregation, mutation, and schema through `UserContext -> TeaQLRuntime -> DataServiceRegistry -> DataServiceExecutor`.
7. Split SQL Provider semantics from SQL Execution Adapter mechanics.
8. Rebuild current Spring SQL path as `SqlDataServiceExecutor + SpringJdbcSqlExecutor`.
9. Rebuild portable SQL path as `SqlDataServiceExecutor + JdbcSqlExecutor` or Android SQLite adapter.
10. Keep current repository APIs as compatibility facades while they delegate to Data Service.
11. Add a no-Spring smoke test and a memory provider smoke test.
12. Add a future Meilisearch provider design or prototype after the routing model is stable.

## Non-Goals

This design does not require immediate removal of `TQLResolver`.

This design does not require immediate removal of `GLobalResolver`.

This design does not require a first-phase distributed transaction protocol.

This design does not require every provider to support every TeaQL operation.

This design does not require shipping an Android SDK module immediately.

## Final Target

TeaQL Java should align with TeaQL Rust's Data Service direction while keeping Java's own center clear:

```text
UserContext-centered core contracts
  + Data Service provider architecture
```

Generated business requests enter through `UserContext`. The runtime routes those requests to named Data Services based on entity metadata and provider capabilities.

SQL, Spring JDBC, direct JDBC, Android SQLite, memory, Meilisearch, and future backends are implementation choices behind that boundary.
