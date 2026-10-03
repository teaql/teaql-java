# Java generated API Trace Chain example

This focused example uses the six-entity KSML model in [model.xml](model.xml),
an unchanged generated domain library, and the runtime from this checkout.
Business creation, graph attachment, deletion, recovery, query and expression access use
generated public APIs. SQLite and the runtime's SQL and committed-audit sinks
provide the acceptance evidence; tests do not inject expected trace frames.

Every successful provider mutation additionally checks `MutationResult.statements()`:
one physical write followed by its actual authoritative SELECT, with the same
typed branch lineage and the originating root's query/request path. Prepared
batches keep a separate statement list per member; concurrent saves do not share
a collection. This checks returned evidence independently of the log sink.
Native SQLite tests exercise all four query/mutation logging combinations.
Disabling diagnostic text does not remove the returned physical facts or change
the number of committed audits. These raw facts are trusted internal diagnostics;
apply `LogPrivacy.sql` before exporting one to a diagnostic sink, never serialize
raw SQL parameters into an application response.

## Run the example

Use Java 21 or newer, Maven, Bash and the normal repository dependencies:

```bash
bash examples/trace-chain/verify.sh
```

The script installs local source dependencies, runs all thirteen scenarios twice
against one database without intermediate cleanup, and compares every generated
library file's SHA256 before and after execution. It prints the retained
directory containing the database, Maven logs and checksum manifests. Set
`TEAQL_TRACE_CHAIN_VERIFY_DIR` to retain subsequent replays in a chosen directory.
The example is also included in both repository example verification scripts
and the `runtime-examples` Maven profile.

## Acceptance scenarios

| Scenario | Observed boundary |
| --- | --- |
| Six-item normative graph | Root update, item update, item deletion, payment insert, attempt insert and shipment insert each retain their own typed lineage in provider commands, actual write/readback SQL and committed audit |
| Three-level query | PaymentAttempt → Payment → CustomerOrder → Platform produces four real SQL queries with ordered field-level relation nodes and the originating comment/purpose |
| Checker rejection | Missing `order_number` fails with its KSML location before provider execution, SQL or committed audit |
| Provider failure | A real SQLite UNIQUE violation rolls back the earlier root insert, retains attempted branch lineage and emits no committed audit |
| Readback failure | A real SQLite failure after a successful update retains separate write/readback outcomes; retry succeeds with the restored optimistic version |
| Prepared insert and ledger replacement | Two generated OrderItems execute in one real two-row JDBC prepared insert with independent command/write/readback/audit lineages; a subsequent update uses a complete ledger chain instead of appending graph fallback |
| Prepared update, delete and recovery | Two identified children with different optimistic versions execute each stage as a real two-row prepared batch, preserving separate command/write/readback/committed-audit lineages; deletion hides them and pure recovery restores both through generated Q/E |
| Overlapping real generated Checkers | With one Context, a valid order commits while an incomplete order fails for `order_number` before allocation/provider access; SQL and committed audit contain only the accepted request's lineage |
| Concurrent independent graphs | Two real threads overlap generated Checker invocations for separate root/child ledgers on one Context and share one unmodified loaded Platform without rebinding its ledger; physical SQLite writer transactions serialize, while each command/write/readback/audit retains only its graph's root and child reason; Q/E reload both commits |
| Concurrent three-level queries | Two live generated queries share one Context without adding ambient frames; each returns its own hydrated objects and four SQL records with only its root intent and logical relation path |
| Late-consumed stream | The real JDBC cursor opens without Context frames; consuming after an unrelated query retains the stream's original comment, purpose, root type and generated E result |
| Nested Facets | PaymentAttempt facets load Payment and its CustomerOrder facet; all five physical queries keep PaymentAttempt as the root, preserve the logical relation route and return the selected payment with count 1 |
| Facet inside a loaded relation | A PaymentAttempt loads Payment and its CustomerOrder facet; all four physical queries keep the original root, including the already-loaded `payment` ancestor |

The first run begins with CustomerOrder and Payment both numbered 100, items
201/202, attempt 401 and shipment 501. IDs come from `IdSpaceIdGenerator`, not
direct entity setters. Replays advance a type-specific floor rather than deleting
rows. The generated bootstrap Platform is reused. Generated checkers are
installed normally and are never replaced with permissive stubs.

Root reason is `submit order`. Payment adds `authorize payment`; its attempt
inherits both. Shipment adds only `dispatch shipment`. The deleted item adds
`remove unavailable item`; the other item inherits only the root reason.
The same numeric ID on two entity types never identifies the same ledger entry.

## Model and API provenance

[AGENTS.md](AGENTS.md) and [retained Assist](evidence/assist/) come from local
model-aware services after [evaluation](evidence/evaluation.md). The generation
fixture is `JavaTraceChainExampleGenerationTest` in the paired generator checkout;
run it with `-Dteaql.java.dir=/absolute/path/to/teaql-java`. Domain-library files
must be regenerated from the model rather than patched by hand.

The test-only `IdDatabase` is a JDBC bridge for the runtime's persistent ID
allocator. It is not a business DAO. Its SQL and failure-injection DDL are
infrastructure; all order/payment data is operated on through generated APIs.

This closes the generated normative graph and three-level SQL path checks for
local Java source, including same-type prepared insert/update/delete/recover batches and a generated
complete-ledger override on an identified existing child. Java assigns IDs at
first graph save; the override probe runs after that insert/readback rather than
inventing a pre-save identity. Current Delete Assist documents `markToRecover()`
followed by audited save; recovery does not need a fabricated scalar-field change.
Native SQLite tests additionally cover stale batch members, multiple update
layouts, detached ledger recovery and rollback when JDBC cannot report exact
per-item optimistic row counts. Native reentrant/concurrent Checker tests also
verify isolated violations, visited identities, Fix evidence and per-graph clock
capture while retaining the original Context for application hooks.
The shared read-only Platform remains in the fixture: its independent ledger
must not be rebound or import another order's pending keys. Related mutation
import uses the explicitly visited type-qualified key. Receiver-owned detached
ledger mutations remain supported; no source ledger is cleared during import.

The verifier now requires thirteen scenario markers. Query provenance is carried
by the validated request and statement, not by a Context push/pop stack or a
ThreadLocal trace. The generated fluent stream API is unchanged; custom provider
implementations must migrate `StreamingQueryExecutor` from a bare SearchRequest
to QueryRequest. Runtime regressions also verify that Policy cannot replace the
captured stream intent and that internal streams inherit intent without repeating
it on their child builder. Legacy direct SQL diagnostics without statement
bindings retain a separate compatibility path; these tests do not establish
its concurrency safety or complete advanced-query/cancellation coverage.

Derived Facet requests inherit both the root intent and the parent's complete
immutable path, rather than rebuilding the origin from the facet entity.
Native `DerivedQueryTraceSqliteTest` separately exercises dynamic aggregates:
filtered counts, an aggregate inside a loaded relation, safe parent-value
redaction, logging disabled, and a numeric partition without a model relation.
Verified relation metadata supplies the reverse-list edge; an arbitrary numeric
partition keeps its parent's path without inventing an edge from the output
metric's name. These are native runtime tests, not generated dynamic-aggregate
acceptance: the retained field Assist does not document that operation.

The synchronous Checker compatibility binding is runtime-internal and carries
no trace or ledger. Nested invocation close restores the parent; the diagnostic
`lastFixEvidence()` receipt belongs to the calling execution thread, not the
shared Context. It is not propagated to another thread or async task. These
focused probes do not prove every auxiliary-table layout, async handoff or
cancellation, all provider/entry-point combinations, complete privacy coverage,
or immutable internal Registry replay. The development dependency version is
not a new public release.
