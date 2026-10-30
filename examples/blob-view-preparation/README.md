# Blob and final-view CLI

Build from the repository root:

```sh
mvn -pl examples/blob-view-preparation -am package
```

Run `io.teaql.examples.blobview.BlobViewExample` using the produced example JAR and
runtime dependencies. Inject TEAQL_BLOB_TEST_PG_URL, TEAQL_BLOB_TEST_PG_USER and
TEAQL_BLOB_TEST_PG_PASSWORD for a dedicated PostgreSQL test database, plus
TEAQL_VIEW_PDF pointing to the shared real PDF fixture. Do not put passwords in
command-line arguments. The example creates/drops only a unique owned schema and
removes its exact temporary file/parser directory on exit.

The host explicitly provisions catalog/command schemas and starts a file-provider
gateway bound to a temporary loopback port. No Web framework or DI container is
used. Policies grant this isolated CLI actor its own scope; replace them with real
current authorization in an application. The example has no secure-document module.

It performs actual upload/content verification, one atomic native SQL+Blob command,
final view preparation and HTTP retrieval using the returned complete URL. It
checks that entities/SQL still contain the Key, nested presentation reuses the same
PreparedView, and storage unavailability after commit preserves a durable receipt
without running the command action again. Credentials and signed URLs are not
printed. The task evidence runner records exact source-built JAR hashes and Java
commands, with no JUnit/test classes on the standalone classpath.
