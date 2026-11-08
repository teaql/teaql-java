# SQLite committed-event source example

## Durable observations (CMR-14)

The same security verifier records nine phases in `observations.json` using
public Context-participating source/target observation APIs. It intentionally
loses both snapshot and tail receipts **after actual target commit**. Target
watermark advances while source ACK/backlog stay behind; duplicate retry then
converges. Two durable journal rows retain six attempts, four retries/failures
and two publisher-observed permanent rejections. Target-wide audit counts five
rejections, including direct unauthorized-source attempts. Final dead letters
are zero, not deleted source facts. Typed observations and existing runtime
telemetry agree; exported attributes contain only counts and hashed routes.
No fake ACK, transport authentication claim or automatic retry worker is used.

## Authenticated snapshot and tail (CMR-13)

`bash examples/committed-events/verify-security.sh` executes the local runtime
example twice; the complete verifier includes it. Two audited Mutation saves,
not INSERT fixtures, produce baseline 1 and tail 2. A retained signed snapshot
bootstraps an independent target. A lost snapshot receipt leaves the source
cursor pending; retry gets a stable duplicate receipt. The signed update then
catches up without rerunning business methods. A source ID above JavaScript's
exact-integer range survives unchanged.

Five real rejections cover bad snapshot tag, tampered tail with recomputed
checksum, unauthorized source, replay of snapshot-covered history and a newer
snapshot attempting to reset an active stream. Checks inspect rows, source
cursor, Inbox, checkpoint and durable hash-only audit, including a fresh target
Context/connection. Exact event/snapshot retries do not rewind the tail.

Builds and disposable fixtures are removed on exit. Explicitly set
`TEAQL_CMR13_EVIDENCE_ROOT` to an absent absolute path to retain two small
SQLite pairs, result JSON and native logs/JUnit. For the complete gate,
`TEAQL_COMMITTED_EVIDENCE_DIR` similarly opts into retaining generated Q proof.
Never reuse/reset these directories. The key is public test material, not a
deployment secret. This is authenticated in-process ingress, not a network/TLS
server, receipt authentication, installed-package or production qualification.

## Existing source and target flows

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

## Consistent snapshot plus tail (CMR-11)

Run `bash examples/committed-events/verify-snapshot.sh`. It copies the current
local source without build outputs, runs `CommittedSnapshotExampleTest` twice
offline and deletes its owned build/database scratch on exit. It never selects
a released runtime. The complete verifier also invokes this gate.

The small fixture has two legacy orders (including a tombstone and an ID above
JavaScript's exact-integer range), a negative epoch timestamp, Boolean/NULL and
an empty entity catalog entry. Context `ensureSchema` creates its schema;
explicit fixture SQL represents old data with no captured journal. A separate
transaction simulates one captured tail. Actual business audited-save capture
remains demonstrated by `CommittedEventsExampleTest`, not by this fixture SQL.

The example imports snapshot N=0, applies tail 1, retries both and proves that a
snapshot retry acknowledges 0 without rewinding checkpoint 1. Optional
`TEAQL_SNAPSHOT_EXPORT_DIR` writes only `snapshot.json`, `tail.json` and their
SHA-256 sidecars with create-new semantics. `TEAQL_SNAPSHOT_IMPORT_DIR` consumes
the same files, including those exported by the Rust example. Use separate
producer/consumer runs and fresh owned export directories; never overwrite old
evidence. These files do not define an authenticated network transport. No
CMR-12 process-crash or production-scale qualification is claimed.

## Managed bootstrap process recovery (CMR-12 candidate)

`bash examples/committed-events/verify-bootstrap-crash.sh` copies the **local**
source, compiles it offline and runs the process example twice. Each invocation
keeps one source database throughout two rounds (snapshot/tail 1/2, then 3/4).
The second target is a new consumer slot, not a reset database. The first target
then catches up to 4 without replacing its original snapshot or rewinding its
cursor. Ordinary business writes use audited saves; one retained legacy
tombstone predates capture.

In each round the owner SIGKILLs and reaps seven distinct workers: during the
capture read transaction, publication retention, partial target installation,
after target commit/before snapshot receipt, between source cursor insert and
ACK commit, partial tail application, and after tail commit/before event receipt.
Fresh workers recover from the same files. SQL barriers exist only in this
test fixture; production has no fault hook. A normal exit without the expected
PID/phase barrier is rejected. Waits and logs are bounded.

Set `TEAQL_CMR12_EVIDENCE_ROOT` to a **nonexistent absolute path** to retain
compact databases, child logs, PID barriers, JUnit XML and `result.json` for both
invocations. No existing path is reset. Otherwise successful disposable test
fixtures are removed after all owned workers exit; failed fixtures print their
diagnostic location. Temporary source/build outputs are removed by the verifier
on exit. This controlled Linux/SQLite **source** example is not installed-package,
power-loss, authenticated transport or whole-CMR-12 acceptance.
