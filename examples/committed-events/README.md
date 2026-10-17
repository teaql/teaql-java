# SQLite committed-event source example

Run `bash examples/committed-events/verify.sh`. It executes a small, real
`Order.save(context)` flow twice against a local Java source. Each round uses
fresh source/target SQLite files and an independent DuckDB target. The test
reopens the Outbox, delivers the two events through bounded publishers into
both model-checked targets, verifies their rows and the SQLite target duplicate
acknowledgement, then injects a later source write failure. No generated ERP
model or remote service is required.

The example configures `SqliteCommittedMutationRecorder` at trusted runtime
startup; `context.ensureSchema()` installs its infrastructure tables. A database trigger
sets `memo` after insert; the Outbox contains that authoritative readback, not
the pre-save entity value. Subsequent update produces sequence 2. When the
Outbox table is made unavailable, the attempted third save rolls back its
business update and emits no committed event.

This is **not** authenticated network replication. The in-process sinks prove
local cursor/target behavior; transport identity, retry budgets, snapshot-plus-
tail and dynamic fields remain separate work. The included typed wire tests
cover selected scalar, null, date, instant and Decimal boundaries, not every
possible model type.
