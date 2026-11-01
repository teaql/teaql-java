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

## Caller-owned Spring transactions

```bash
node examples/conformance/spring-transactions/verify-local.cjs
```

This focused source-paired gate reuses the unchanged two-object generated library.
Business operations use generated Q/E and audited Save, with an application-owned
Spring `DataSourceTransactionManager` over a real SQLite file. Eight scenarios
run twice on the same stores: sparse no-op plus outer commit; outer rollback and
latest-intent retry; nested rollback; equal-position/repeated savepoints;
`REQUIRES_NEW` isolation; post-commit audit delivery failure; pre-provider Checker
and intent rejection; and an actual second graph-write failure followed by retry.
Independent SQL is used only for readback and controlled trigger fault injection,
not to implement application persistence. Audit observers verify committed rows
through an independent connection, outside the completed Spring owner.

Set `TEAQL_JAVA_SPRING_EVIDENCE_DIR` to a new absolute directory to retain compact
logs, JUnit XML, hashes and databases there. Builds and isolated dependency copies
are removed on exit; no TeaQL artifacts are borrowed from the Maven cache and no
shared install/deploy is performed. `--baseline` executes the same application
against the pre-fix `6d162bb0` production archive and intentionally exits nonzero
when it reproduces a failed assertion; that is not a passing runtime gate.

The complete example script includes this gate. This is SQLite plus Spring JDBC,
not JTA/reactive/custom-manager or installed-artifact acceptance. The model has no
Decimal field: exact Decimal registration for Spring-backed SQLite is a separate
provider boundary, not implemented by a fake function in this example.
