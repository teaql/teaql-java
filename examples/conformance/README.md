# Java minimum runtime conformance example

This retained SQLite workspace is generated from `model.xml`. It verifies
explicit `ensureSchema`, Checker rejection before persistence, Create, typed Q
and `SmartList`, E loaded/null/not-loaded semantics, Update/version, and Delete.
It also proves that optimistic versions remain isolated when different entity
types use the same numeric ID and their mutation ledgers are merged.

```bash
make run
```

The generated Runtime Module is installed at application startup but remains a
passive manifest. Schema reconciliation is invoked separately and explicitly.
