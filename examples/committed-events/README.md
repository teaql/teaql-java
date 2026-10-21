# SQLite committed-event source example

Run `bash examples/committed-events/verify.sh`. It executes a small, real
`Order.save(context)` flow twice against a local Java source. Each round uses
fresh source/target SQLite files and an independent DuckDB target. The test
reopens the Outbox, delivers the two events through bounded publishers into
both model-checked targets, verifies their rows and the SQLite target duplicate
acknowledgement, then injects a later source write failure. The first SQLite
delivery deliberately loses its acknowledgement after target commit; a new
source executor replays sequence 1 as a duplicate and then applies sequence 2
in order. This small fixture
itself requires no generated ERP model or remote service.

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

The verifier also runs an application-owned generated Standard Order consumer
twice. It creates a Customer through generated audited Save on SQLite, delivers
one committed set to a model-checked DuckDB target, and reads the target through
generated Q. The checked-in generated library remains byte-for-byte unchanged;
public-package and production-network qualification are separate.

## Real source-process crash (CMR-03)

`bash examples/committed-events/verify-process-crash.sh` runs a separate bounded
fixture (also included by the complete verifier above). An actual child performs
a normal audited save, signals a post-commit/pre-publish barrier, and waits with
the runtime still open. Its owning test forcibly terminates and reaps it. A new
process recovers the durable Outbox, lands the event, simulates a lost target
acknowledgement, accepts the duplicate receipt and advances the source cursor.
A third process checks persistent rows, versions, Inbox and cursor again.

The same source/target files are used for both rounds: their row/event/cursor
counts go from one to two, not back to zero. A business-invocation marker also
stays one-per-writer, not one-per-delivery. A negative control rejects normal
exit without a ready marker. Logs and waits are bounded; only owned children
are killed. No production callback, HTTP endpoint or new runtime API is added.

Set `TEAQL_CMR03_EVIDENCE_DIR` to a **fresh** directory to retain the two small
databases, child logs, barriers and `result.json` at a known location. By default
an owned temporary evidence directory is printed and retained. Never delete or
reset a previous database to force success. Module `target` directories remain
ordinary reusable Maven build outputs; one-off gate owners clean their own
build directories after recording results. This proves controlled process death,
not machine/power failure, snapshot restart, all CMR-12 paths or public artifacts.
