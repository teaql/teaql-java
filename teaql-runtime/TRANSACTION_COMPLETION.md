# Graph saves inside caller-owned transactions

The direct-JDBC physical owner notifies runtime when the actual outer transaction
commits, rolls back, or has an unknown outcome. `SqlDataServiceExecutor` exposes
the exact owner's identity and completion capability. A nested transaction return
does not authorize committed audit or destructive ledger cleanup.

Before business writes, runtime registers a recoverable attempt so a failed first
write followed by an unconfirmed rollback cannot lose UNKNOWN protection. After a
successful graph write, runtime retains an `EntityMutationLedger.Submission`
containing that batch's fields, lifecycle intent, original versions and trace data.
The current change set starts a new unsubmitted batch; the submitted intent is
still retained in the journal. `hasUncommittedChanges()` distinguishes this state
from a completed save. Tentative authoritative IDs/versions/values are applied
inside the transaction, so another save in the same owner can use the correct
optimistic version. They are not a claim that the outer transaction committed.

The physical owner first runs state preparation for every registration; only
after all graph state is settled does it deliver any completion notification.
The preparation phase contains no business I/O or delivery reentry. On physical
commit, submitted journal batches retire before audit delivery, while
later unsubmitted edits remain dirty. Commit callbacks do not reapply old readback
or erase later fields. Each batch's audit identity, resulting version, actor,
category, reason, field values and trace are captured before later edits. Delivery
uses a commit-time timestamp. Standard maps/collections, JSON trees, byte arrays,
dates and relation identities are detached for audit snapshots; cyclic or oversized
structured values fail before a transactional save can commit. Custom opaque
mutable audit value classes need their own immutable representation.

Notifications for separate batches and entities continue after a delivery failure.
The physical owner reports COMMITTED if notification fails after successful commit;
runtime keeps committed persistence state and does not automatically replay writes.
Audit delivery can start an independent graph save. An older callback never
overwrites the version returned by that later transaction.

Rollback unwinds journal batches in reverse order. Runtime restores the earliest
database version and retained mutation/lifecycle intent, incorporating later user
edits. Blob binding, ordinary fields and optional dynamic fields stay under the
same actual SQL transaction. A create followed by delete can cancel creation on
rollback; a later explicit edit can create that object again. Delete/recovery
intent is rebased onto the original live/deleted row state. Stable allocated IDs
remain available for a deliberate retry.

UNKNOWN keeps the journal and tentative state marked uncertain, delivers no
committed audit, and rejects another save through that entity's ledger. It does
not guess rollback or authorize a blind retry. The caller must reconcile durable
database/idempotency state and reload an appropriate graph. This in-memory guard
does not replace durable command idempotency across process failure.

Public ledger discard and change-set stack operations reject while a submission
is pending. A dirty shared entity may move between roots under the same verified
physical owner. Only that entity's working intent moves; other unapproved fields
remain in their source ledger. Submitted history joins a state-only cohort whose
rollback routes each entity to its current holder in physical submission order.
Earlier empty graph ownership is released before any audit can reenter, including
for independent graphs under the owner. Direct unvalidated ledger reassignment,
foreign owners and independently submitted conflicting histories reject before
a move. Use a canonical mutable entity when composing related graphs. The full
graph/cascade and durable idempotency matrices still require separate acceptance.

Blob saves require a transaction executor with physical completion support and an
active opaque identity. Unsupported providers reject before business writes.
Ordinary legacy executors without this capability keep their existing path; this
document does not establish caller-owned transaction guarantees for them. Host-
managed/Spring/other adapters need their own physical owner implementation.
Framework-internal journal methods are not business commit or retry credentials.

`TransactionJournalTest` checks retained batches, reverse restoration, unknown
results and ownership/discard guards. `AuditSnapshotTest` checks aliasing and
mutation of deferred structured values. `BlobOuterTransactionIT` uses real
PostgreSQL, native graph saves and independent connections for commit/rollback,
multiple create/replace/clear batches, late edits, inner failure, failed audit,
delete/recovery, deferred commit failure, actor capture and audit reentry. It also
combines actual JDBC dynamic fields with Blob binding in the same transaction.
Full Blob module assembly, remaining graph/idempotency gates, view preparation,
Rust and release stages remain separate incomplete R3–R8 work.
