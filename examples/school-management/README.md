# School Management bootstrap example

This generated example retains `models/school-model.xml`. It explicitly calls
SQLite `ensureSchema` twice and verifies Platform `id=1` plus SchoolType constants
`1001`/`1002` are present exactly once with version 1.

Before publication, install the repository's local runtime and then run the
generated workspace. The portable SQL runtime test separately changes a constant
and verifies optimistic, single-version reconciliation.
