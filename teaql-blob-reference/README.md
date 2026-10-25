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
