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

This first extraction preserves Java's existing entity-reference capability.
Full Java document snapshot/open/accept and Blob binding/selection integration
remain R5 work; the module name does not claim those gates have passed. Rust's
existing tqd1 compatibility remains independently verified in its own crate.
