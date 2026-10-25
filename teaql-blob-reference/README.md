# Optional Blob reference module

Tracking [Java #272](https://github.com/teaql/teaql-java/issues/272) and
[conformance #237](https://github.com/teaql/teaql-conformance/issues/237).

R3 implementation is in progress. This first unit provides trusted immutable
profile overrides, resolved policy snapshots and pure retention/deadline rules.
`BlobPolicies` resolves field profiles over module defaults and built-in defaults,
intersects formats with model/deployment limits, and caps read credentials without
changing retained object lifetime. Historical retention promises take the maximum.
Private cache settings are explicit; automatic public caching is rejected.

The shared policy fixtures verify these components. They do not establish upload,
catalog/schema, same-physical-transaction binding, real storage or HTTP read
authorization acceptance. In particular, a known Key or a resolved profile is not
a read/binding credential. The module does not yet register `blob.v1`; runtime
assembly, store/catalog adapters and their integration gates are the next units.

Concrete transport remains optional. The low-level object storage SPI adds an
explicit version-conditional-delete capability; unsupported providers reject.
The S3 adapter uses the SDK's `DeleteObject.ifMatch` and preserves precondition
conflicts without an unconditional fallback, following
[Amazon's DeleteObject contract](https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html).
The loopback HTTP/SDK negative control passes; each actual S3-compatible backend
still requires its own precondition conformance check.

The base runtime now offers a lightweight `BlobMutationParticipant` save SPI.
`plan` runs after approval; a pure resource check runs before opening the business
transaction, and `prepare`/`apply` run inside it. Apply receives immutable planned
and persisted identity/revision mappings. Missing participants and incompatible
transactions fail before business writes. The optional provider must additionally
verify the exact SQL adapter and its active transaction; the runtime does not infer
physical atomicity from a matching database address.
Save also checks installed manifest requirements when the host skipped startup
readiness. A participant alone remains insufficient to satisfy full `blob.v1`.
Missing persisted IDs/revisions reject before binding apply/commit. A failure in
post-commit audit delivery never makes the participant repeat business/binding writes.

`BlobBindingIntents` extracts typed assignments from that approved plan and current
context metadata without reading entities or storage. Updates preserve omitted
fields; nullable explicit nulls clear; creates require real keys for required fields.
Delete/recovery intents carry the field surface for authoritative catalog resolution.
In-memory save-boundary tests cover phase ordering, rollback, retry and final IDs;
these are component checks, with real SQL catalog/locking tests still pending.
