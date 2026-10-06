# School Management bootstrap example

This generated example retains `models/school-model.xml`. It explicitly calls
SQLite `ensureSchema` twice and verifies Platform `id=1` plus SchoolType constants
`1001`/`1002` are present exactly once. Fresh rows start at version 1.

The application-owned `BootstrapTraceVerifier` observes the generated bootstrap's
real SQL intent and committed audit lineage, without injecting trace frames.
It changes PRIMARY's name through audited Mutation, then calls ensureSchema to
restore the model-defined name. Each edit/repair advances the version once;
SECONDARY remains unchanged. Repeated ensureSchema performs lookups only, emits
no mutation audit, and restores the caller's bootstrap audit attributes. Both
fresh and already-seeded databases are exercised by the two-start verifier.

The application-owned `SchoolLifecycleVerifier` also checks a missing required
name is rejected by Checker before mutation or schema SQL during `save`,
using a SQL-log capture in the checker probe. It then exercises audited create,
loaded E traversal, full-field update, mark-for-deletion plus save, and normal-query
absence. The example gate runs with a fresh SQLite database; running it a
second time against the same database is supported.

`RequestIntentVerifier` proves generated Q requests and graph saves reject
missing comment/purpose before policy or SQL. It covers Unicode whitespace,
fabricated ambient intent and root intent inheritance through both forward
relations. Unit tests separately prove the gate is independent of logging.

For updates, load the complete scalar entity. A read projection that includes
only selected fields of related entities is useful for E/display, but should
not be reused as the mutation graph: Checker correctly rejects those partial
related entities as `NotLoaded`.

Before publication, install the repository's local runtime and then run the
generated workspace. Portable provider tests forbid the removed raw seed APIs
and prove physical DDL never inserts/reconciles root or constant data. Typed
constant reconciliation is verified here, through the same generated API as an
application, rather than through a lower-level raw SQL shortcut.

From the repository root, run `examples/verify-runtime-examples.sh`. The gate
builds both retained examples against the current reactor sources, assigns each
example an isolated temporary SQLite database, runs each twice without cleanup
between repetitions, and requires both the lifecycle and request-intent markers.
It exits non-zero if either application fails or times out.
