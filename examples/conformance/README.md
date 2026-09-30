# Java minimum runtime conformance example

This retained SQLite workspace is generated from `model.xml`. It verifies
explicit `ensureSchema`, Checker rejection before persistence, Create, typed Q
and `SmartList`, E loaded/null/not-loaded semantics, Update/version, and Delete.
It also proves that optimistic versions remain isolated when different entity
types use the same numeric ID and their mutation ledgers are merged.

```bash
examples/verify-runtime-examples.sh
```

Run the command from the repository root. It builds against current reactor
sources and verifies this example together with the School bootstrap example,
using an isolated temporary SQLite database for each run.

The generated Runtime Module is installed at application startup but remains a
passive manifest. Schema reconciliation is invoked separately and explicitly.
