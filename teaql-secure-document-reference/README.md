# Optional secure document reference module

Tracking: [Java #272](https://github.com/teaql/teaql-java/issues/272),
[conformance #237](https://github.com/teaql/teaql-conformance/issues/237).

Add `io.teaql:teaql-secure-document-reference` explicitly for governed operations.
The base runtime has only lightweight SPI/context facades and does not depend
on this artifact. Java 17, terminal programs and ordinary UserContext assembly
work without a Web framework or container.

Construct the existing `AeadRoundTripReferenceProvider` with the same key ring,
deployment configuration and current authorization policy. It now lives in
`io.teaql.secure.document.reference`, together with key/configuration types.
Install it using `SecureDocumentReferenceModule.of(provider).installInto(builder)`.
This registers only the provider; it does not load or issue documents, migrate
schema or start jobs. Duplicate installation rejects. After installing business
manifests, call `runtime.validateCapabilities()`. A manifest declaring
`secure_document.v1` fails without a provider; context reference calls also fail
closed. Ordinary mode does not enable the unsafe raw diagnostic switch.

Source imports and dependencies must be updated together; existing governed
applications retain their protection mode and keys. tqr1 behavior and golden
vectors are unchanged. The original tests moved with the implementation.

Java can now issue and consume the original `tqd1` document snapshot through
`UserContext.issueDocumentSnapshot(snapshot, lifetime)` and
`UserContext.consumeDocumentSnapshot(token, expectedAggregateType, expectedPurpose)`.
The snapshot records the model fingerprint, business ID, aggregate, loaded and
writable fields, allowed mutations, and relation completeness. The current
trusted principal and application authorization policy are checked at issue
and consume; a valid token alone does not authorize an edit. The `tqd1` wire
matches the Rust implementation byte-for-byte under fixed test inputs, while
the existing `tqr1` vector stays unchanged. Do not log complete tokens.

The core now exposes an application-owned `ContextBoundDocumentService` SPI and
`UserContext.openDocument` / `acceptDocument` facades. The focused SQLite Order
example implements current authorization, version checks, editable projection
validation, and transactional command receipts. The secure module itself remains
passive: the application must install its own service and persistence boundary.
Generated-model integration, governed contribution for newly prepared editable
graphs, versioned Blob selection, and the four module combinations remain R5 work.

The focused SQLite application consumer now also exercises the unified
`ViewPreparationService` handoff for a previously issued document. It opts in
through `ContextBoundDocumentService.supportsViewPreparation()` and checks
current application authority in `validatePreparedDocument`. The optional
secure module itself still installs only the provider; it does not start a
document service or change all responses to governed mode.
