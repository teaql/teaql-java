# School Management bootstrap example

This generated example retains `models/school-model.xml`. It explicitly calls
SQLite `ensureSchema` twice and verifies Platform `id=1` plus SchoolType constants
`1001`/`1002` are present exactly once with version 1.

The application-owned `SchoolLifecycleVerifier` also checks a missing required
name is rejected by Checker before mutation or schema SQL during `save`,
using a SQL-log capture in the checker probe. It then exercises audited create,
loaded E traversal, full-field update, mark-for-deletion plus save, and normal-query
absence. The example gate runs with a fresh SQLite database; running it a
second time against the same database is supported.

For updates, load the complete scalar entity. A read projection that includes
only selected fields of related entities is useful for E/display, but should
not be reused as the mutation graph: Checker correctly rejects those partial
related entities as `NotLoaded`.

Before publication, install the repository's local runtime and then run the
generated workspace. The portable SQL runtime test separately changes a constant
and verifies optimistic, single-version reconciliation.
