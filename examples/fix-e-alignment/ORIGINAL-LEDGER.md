# Direct original Mutation Ledger clauses

Run `node examples/fix-e-alignment/verify-local.cjs --original-ledger` against
repository-local runtime modules. The default Fix/E example also selects this
test alongside every previous test.

The same seven ML-01 through ML-07 clauses run twice on one retained SQLite
store. They cover independent new roots, deferred B saving, clearing only A's
committed intent, two separate queries for the same ID, explicit typed child
attachment, and an actual trigger-induced save failure. B must still save while
A is failed; then the same A object retries with its original optimistic version.

Business writes use generated mutation/audited Save, and business readback uses
typed Q/E. Direct SQL only observes storage or installs/removes the named fault
trigger. Runtime origins are asserted and class bytes are reported. The runner
preserves the evaluated model and generated library hashes, retains logs, JUnit
and the database, and removes its disposable source reactor and build outputs.
This does not qualify ML-08 through ML-13 or a published package by itself.
