# Business ID focused runtime example

This example verifies the new Java runtime Business ID boundary without running
the full multi-module regression suite. It covers the default eight-digit
daily sequence, controlled Context date, root scoping, Mutation Ledger retry
stability, explicit SQLite schema setup, cross-instance allocation, restart,
strong `OrderNumber` parsing, and immutable established aggregates.

The preferred contract is `context.businessIds().ensure(context, definition,
domainRootKey, aggregateType, slot)`. Install the allocator with
`TeaQLRuntime.builder().businessIdInfrastructure(allocator)`, then explicitly
call `context.ensureSchema()` before allocation. The business date comes from
the context's `BusinessClock`; tests can replace that clock. The JDBC allocator
uses root/aggregate/namespace/date scoping, optimistic allocation, bounded
retry, and a range error when the configured digits are exhausted.

`BusinessIdGenerator`, `UserContext.generateBusinessId(...)`,
`InMemoryBusinessIdGenerator`, and `JdbcBusinessIdGenerator` are deprecated
compatibility APIs. They retain the old `PREFIXyyyyMMddNNNN` format and separate
sequence namespace; do not mix their sequence state with `BusinessIdService`.
The legacy JDBC implementation no longer creates a table in its constructor or
during allocation. If an existing integration must keep it, register it as a
`BusinessIdSchemaContributor` on the context and call `context.ensureSchema()`
explicitly. Both legacy implementations now use `context.businessDate()` and
reject an exhausted digit range. Repository source and generated-template
search found only the public API and tests using the legacy path; external
consumers have not been inventoried and must be treated as unknown.

```bash
mvn -pl examples/business-id-runtime -am \
  -Dtest=BusinessIdRuntimeExampleTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```
