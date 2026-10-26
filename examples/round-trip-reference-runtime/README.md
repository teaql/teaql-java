# Round-Trip Reference runtime example

This example proves that the reusable runtime serialization boundary can be
called by any Web adapter without depending on Spring, JAX-RS, Servlet, or a
specific JSON library.

It covers governed issue/resolve through `UserContext`, trusted Actor and
Domain Root binding, purpose/document/Aggregate scope, tamper rejection,
expected-type validation, expiry, key rotation, current authorization at issue
and consume time, and the explicit development/test-only raw diagnostic shape.

The core deliberately does not define tenant or role semantics. Runtime
assembly installs the trusted principal and application-owned authorization
policy. The focused runtime tests retain the same deterministic `tqr1` golden
vector as TeaQL Rust and enumerate RAW-01 through RAW-09.

`SqliteOrderDocumentService` is an application-owned, framework-free `tqd1`
open/accept consumer. It reads current permissions and a complete Order/items
projection from a real SQLite database, issues the protected snapshot, and
accepts only the issued writable quantity field or an explicit complete-list
removal. The accept path checks current ownership, item and aggregate versions,
and writes the Order revision, item changes, and command receipt in one SQL
transaction. A repeated identical command returns its first result; a changed
request with the same command ID conflicts. The focused SQLite test covers a
failed item write rolling back both the Order revision and command receipt.
The public document DTOs freeze nested JSON values before digesting or saving,
and the service rejects a directly supplied principal that differs from the
current `UserContext` principal.

This example demonstrates the Java document SPI and actual SQL consumer. It
does not implement generated Order models, governed ViewPreparation provenance,
Blob field selection, or the four module combinations required for R5 exit.

Run locally against repository sources:

```bash
mvn -pl examples/round-trip-reference-runtime -am test
```
