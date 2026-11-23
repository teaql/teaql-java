# Java minimum runtime conformance example

`model.xml` owns `java-lib-core` and `java-app-console`. The original application and Spring transaction business source now live in the native workspace. `bash examples/conformance/verify.sh` uses source-built runtime/domain artifacts with isolated offline Maven, runs both lifecycle and eight Spring scenarios twice on retained databases, and removes owned scratch. See `showcase.json` for actual qualification state.


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

## Generated Fix postconditions and safe provenance

```bash
node examples/fix-e-alignment/verify-local.cjs
```

The unchanged three-object model supplies a parent and child with required
context-root references and create/update timestamps. Three additional
application-owned tests assert typed managed defaults before validation,
unchanged ordinary business inputs read back with generated Q/E, and exactly
one immutable, value-free provenance entry per fixed field. Invalid required
and range inputs fail after Fix with zero provider reads, writes or transactions;
corrected input then saves in a fresh invocation. Both rounds use the same
SQLite files without cleanup between them.

`TEAQL_FIX_E_EVIDENCE_DIR` selects a new absolute directory for compact logs,
hashes and databases. Build output is removed on exit. This proves the declared
clock/root and ordinary required/range vectors, not arbitrary custom context
fixers, a range-constrained generated default, security/TTL clock behavior or
the complete seven-runtime diagnostic contract.

## Context-derived fields

The focused Context-derived field gate is:

```bash
node examples/conformance/context-derived/verify-local.cjs
```

It compiles an unmodified two-object generated library with local runtime
sources and runs typed Q/E and audited Save against one SQLite file twice.
Different request time, IP, and actor do not update an unchanged object.
A real edit applies update-scoped Context values and preserves create-only
values. Failed validation restores automatic Context fields while retaining
the caller's edit, which can be corrected or reverted. The application-owned
Checker and metadata customization demonstrate the same guard for non-clock
Context fields; generated timestamp assignments use that guard too.

The same fixture also loads typed readonly query properties and persistent
extensions. A readonly property does not dirty the entity. A dynamic-only edit
does apply update-scoped Context values, while assigning its known persisted
value again leaves SQL, version and committed audit unchanged. The extension
provider shares the exact JDBC executor used by the owner graph transaction;
there is no hand-written mutation SQL or separately committed extension write.

`TEAQL_CONTEXT_FIX_EVIDENCE_DIR` chooses a new directory for compact receipts,
logs, hashes and the SQLite store. Temporary source/build consumers are removed
on exit. This is local source acceptance, not a public-package release or
completion of every graph/dynamic-field case in all seven languages.

## Natural Checker messages

```bash
node examples/conformance/natural-messages/verify-local.cjs
```

The [focused example](natural-messages/README.md) uses a separately evaluated,
two-entity generated model and local runtime sources. Two rounds share one
SQLite store. Actual generated Q/E and audited Save verify human-readable
English/Chinese child-field messages, unchanged machine pointers and rule IDs,
original UTF-16 validation length after masking, rejection before provider/audit
calls, automatic Context rollback and successful correction. Presentation
metadata is attached through the runtime module seam; automatic generator
emission and published-package acceptance remain separate gates.

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
