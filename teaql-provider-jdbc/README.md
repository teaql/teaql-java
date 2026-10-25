# Direct JDBC transaction completion

`JdbcSqlExecutor` owns connections obtained from its configured DataSource. Nested
`executeInTransaction` calls on the same thread borrow the current connection and
opaque transaction identity. The nested return does not imply physical commit.
Connections managed independently by a host or another adapter are outside this
ownership protocol.

The lightweight `TransactionCompletion` SPI and optional
`supportsTransactionCompletion`/`afterTransaction` methods expose physical
completion to infrastructure. `SqlDataServiceExecutor` delegates them to the exact
execution adapter. Unsupported adapters report false and reject registration;
they cannot approximate completion with a nested return. Registration requires an
active physical transaction. Constructors and capability checks do no I/O.

The owner clears transaction thread state and closes its connection before
delivering notifications. Commit notifications run in registration order.
Rollback and unknown notifications run in reverse order, so staged state can be
unwound. Every registered notification runs once even if another fails; failures
are retained through cause/suppression. A callback can open a new independent
transaction. No actor, module, thread, queue worker or scheduler is retained after
completion.

| Outcome | Meaning |
| --- | --- |
| COMMITTED | JDBC `commit()` returned successfully; later reset/close/notification failure cannot undo it |
| ROLLED_BACK | No commit was attempted and rollback succeeded, or execution never began |
| UNKNOWN | Commit acknowledgement or rollback failed; no committed delivery or automatic business retry is authorized |

`TransactionCompletionException.outcome()` preserves the physical outcome when
commit, cleanup or notification fails. Known business runtime failures after a
successful rollback keep their original structured exception, with any cleanup
or callback failures suppressed. Fatal errors remain fatal after notification
draining. A failed commit remains UNKNOWN even if a later rollback succeeds:
the database may already have committed before the response was lost.

A failed nested action marks the shared physical transaction rollback-only.
Catching that exception in the outer action cannot commit earlier partial writes.
The root reports ROLLED_BACK after successful rollback. There are no implicit
savepoints. If independent recovery is needed, the host must start a separate
physical transaction with an explicit consistency contract.

Auto-commit restoration occurs only after commit or rollback returned successfully.
This avoids a cleanup operation committing work after an unconfirmed rollback;
changing auto-commit mode has transactional effects under the
[JDBC Connection contract](https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/java/sql/Connection.html).
The original failure remains available when rollback or resource cleanup also
fails. Pool eviction and loss-of-connection reconciliation remain host concerns;
an UNKNOWN result must be reconciled through durable state/idempotency.

`JdbcTransactionCompletionTest` uses actual H2 transactions with non-reflective
driver subclass faults before/after actual commit, on rollback, reset and close.
Independent connections inspect durable rows; tests also cover nested ordering,
rollback-only, callback failure/draining/reentry and concurrent identity isolation.
`PostgresTransactionCompletionIT` in the optional reference module adds actual
PostgreSQL catalog/business writes, independent SQL oracles and a real deferred
foreign-key failure at commit. Test DDL belongs only to its owned test schema.

This is a transaction-owner substrate. Runtime graph ledger/audit completion,
repeated saves within caller-owned transactions, reconciliation and complete Blob
module assembly require their own integration. This API alone does not establish
full `blob.v1` or R3 acceptance, and other adapters must implement and prove their
own physical completion behavior.
