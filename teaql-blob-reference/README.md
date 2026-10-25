# Optional Blob reference module

Tracking [Java #272](https://github.com/teaql/teaql-java/issues/272) and
[conformance #237](https://github.com/teaql/teaql-conformance/issues/237).

R3 implementation is in progress. Current components provide trusted immutable
profile overrides, resolved policy snapshots and retention/deadline rules.
`BlobPolicies` resolves field profiles over module defaults and built-in defaults,
intersects formats with model/deployment limits, and caps read credentials without
changing retained object lifetime. Historical retention promises take the maximum.
Private cache settings are explicit; automatic public caching is rejected.

The shared policy fixtures verify these rules. The PostgreSQL catalog and save
participant additionally have a real SQL component gate described below. Production
upload verification, real object-store I/O and HTTP read authorization remain pending.
A known Key or a resolved profile is not a read/binding credential. The module does
not yet register `blob.v1`; full runtime assembly and its remaining service gates
are still in progress.

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
In-memory save-boundary tests cover phase ordering, rollback, retry and final IDs.

`PostgresBlobCatalog` uses explicit, transactional, fixed version/checksum migrations
for its own infrastructure tables. It checks columns and named primary/unique/foreign
key constraints. This is separate from model schema reconciliation; no general DDL
diff or global model registry is introduced. Constructors never open connections.
Upload begin/complete catalog transitions are idempotent, with immutable policy
snapshots; `VerifiedContent` is a trusted internal input from the future content
verifier, and is not evidence that a client uploaded valid bytes.

`PostgresBlobMutationParticipant` borrows the exact SQL adapter and its opaque active
physical transaction identity. It locks target bindings and sorted old/new object
keys, intersects current host field permission with an additional Blob policy,
checks scope/formats/size/state/deadlines/sharing, and applies changes before commit.
Empty/deleted target rows retain monotonic binding versions; a retired Key supports
authoritative recovery and grants no read authority. Accepted retention maxima survive
unbind. A logical deleting claim rechecks live references under the same object lock;
physical version-conditional deletion is still a separate pending maintenance stage.

The explicit opt-in `PostgresBlobCatalogIT` suite runs against a real PostgreSQL:
native business saves plus independent-connection SQL oracles, commit/rollback,
recovery/clearing, scope and both permission layers, expiry/retention, concurrent
single-binding winners and GC lock competition, cross-transaction Prepared rejection,
post-commit audit failure, and actual compiled/generic typed SQL hydration. It requires
`TEAQL_BLOB_TEST_PG_URL`, `TEAQL_BLOB_TEST_PG_USER`, `TEAQL_BLOB_TEST_PG_PASSWORD`, and
`TEAQL_BLOB_TEST_FIXTURES` pointing to the shared real-file fixture directory. It does
not establish public artifact, generated-workspace, caller-owned Java transaction
completion, production MIME/container validation, or full R3 acceptance.
