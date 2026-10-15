# SQLite committed-event source example

Run `bash examples/committed-events/verify.sh`. It executes a small, real
`Order.save(context)` flow twice against local Java source. Each round uses a
fresh SQLite file, and each test reopens the Outbox before injecting a later
write failure. No generated ERP model or remote service is required.

The example configures `SqliteCommittedMutationRecorder` at trusted runtime
startup; `context.ensureSchema()` installs its infrastructure tables. A database trigger
sets `memo` after insert; the Outbox contains that authoritative readback, not
the pre-save entity value. Subsequent update produces sequence 2. When the
Outbox table is made unavailable, the attempted third save rolls back its
business update and emits no committed audit event.

This is **not** a completed replication example: it does not publish the row,
validate a target model, apply to another runtime, or cover dynamic fields.
