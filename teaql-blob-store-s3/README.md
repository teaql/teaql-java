# Optional versioned S3 Blob transport

`teaql-blob-store-s3` implements the core `BlobStore` SPI. Its production TeaQL
dependencies are core and the private staging helper. SQL, runtime assembly, Tika,
and secure-document implementations remain separate. Tracking
[Java #272](https://github.com/teaql/teaql-java/issues/272).

The host supplies a dedicated namespace in an already versioning-enabled bucket,
SDK credentials, region, private local scratch directory and maximum read lifetime.
Construction is passive. `validateReady()` explicitly checks bucket access and
versioning; it never creates a bucket, changes versioning, migrates SQL or starts
workers. Each publication and retirement also rechecks versioning. Unversioned or
suspended buckets are unsupported. The configured prefix is trusted host input;
upload IDs map to SHA-256 object names, independently of client filenames.

`connect(endpoint, readEndpoint, region, credentials, bucket, prefix, scratch,
maximumReadTtl, pathStyle)` creates owned SDK clients with bounded network timeouts.
`readEndpoint` is optional: it configures the SDK presigner's actual external
endpoint. Applications consume its complete URL without rewriting hosts, paths or
query strings. A supplied client/presigner transfers ownership to the constructor;
`close()` closes them. The host retains ownership of its credentials provider.
Provider configuration is trusted; readiness does not replace the host's IAM and
network setup.

Writes use a bounded, repeatable private file and conditional `PutObject` with
`If-None-Match: *`. Identical retries return the existing generation; different
size/hash conflicts cannot overwrite it. The caller owns the source stream.
This implementation uses a single PUT, caps it at 5 GiB, and does not implement
multipart or direct browser uploads. Public Blob services must still enforce
their policy limits and verify the actual stored bytes before making a Key
bindable. `stagingMaintenance(permission)` uses the shared helper's current global
scratch permission, independently of tenant catalog maintenance.

Content identity is the backend's non-null `versionId`, rather than an ETag: two
writes of the same bytes can have identical ETags and different generations.
Ranges and full signed GET URLs include that exact version. A replacement cannot
redirect an already issued URL to different bytes, following the
[S3 version retrieval contract](https://docs.aws.amazon.com/AmazonS3/latest/userguide/RetrievingObjectVersions.html).
The range stream aborts unread SDK responses when closed. Signatures contain the
actual verified content type, private cache policy and encoded download filename.
The effective lifetime is capped by the request, provider maximum (at most seven
days) and known credential expiry. Signing captures one credential identity;
session credentials without known expiry fail explicitly. Credentials, object
locations and complete bearer URLs are absent from value `toString()` output.

Collection first commits the catalog's `deleting` claim. `sealForDeletion` then
conditionally publishes a small persistent ordinary object at the same key with
`teaql-blob-state=retired-v1`. It records the selected payload version. An absent
key uses conditional creation; an existing pinned payload uses its current ETag
as the compare-and-set precondition. Conflicts are bounded and never fall back to
an unconditional mutation. The ordinary retirement object blocks a remote
immutable PUT that finishes after its client died and released the SQL lock.
An S3 delete marker cannot supply this fence, because conditional creation can
succeed when that marker is current, per
[S3 conditional-write semantics](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-writes.html).

Before deleting a payload, the adapter examines at most 64 returned version
entries for that exact key. Truncated history, ordinary S3 delete markers, or
another payload generation report a conflict. If a same-ETag replacement won
between HEAD and retirement publication, the adapter removes its own retirement
version and preserves both payloads. The actual delete specifies only the selected
`versionId`; it cannot remove another generation, per
[S3 DeleteObject](https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html).
Retries retain the same selection and accept an already missing selected payload.
Read/stat hide retirement objects. Retired upload targets cannot be reused.

The namespace must have no other writers or lifecycle rule that expires these
retirement objects, inserts delete markers, or removes versions behind the catalog.
The tiny fence persists after payload deletion; this release does not provide an
online fence compactor. Removing it requires an independent proven quiescence
and namespace-retirement procedure. Unexpected history fails conservatively.
Logical `deleted` means the selected payload was removed or already absent; it
does not claim destruction of backups, replicas, foreign versions or the fence.
This design is for versioned general-purpose S3-compatible buckets, not directory
buckets or a claim that all vendors implement the same consistency semantics.

Host permissions must allow bucket access/versioning reads, scoped object
version listing, content/metadata reads, conditional puts and deletion of specific
versions. Bucket creation/versioning administration belongs to provisioning and
is unnecessary in the adapter. A backend must prove its conditional writes,
version isolation and listing behavior before deployment.

`S3BackendIT` and `S3BlobStoreIT` are opt-in actual-backend tests. Supply
`TEAQL_BLOB_S3_ENDPOINT`, `TEAQL_BLOB_S3_BUCKET`, `TEAQL_BLOB_S3_ACCESS_KEY`,
`TEAQL_BLOB_S3_SECRET_KEY`, and `TEAQL_BLOB_TEST_FIXTURES`. Test provisioning enables
versioning in that dedicated test bucket; teardown removes only each test's unique
prefix. They cover ten exact real files, input limits, retries/concurrent writers,
versioned reads and deletion, same-ETag races, delayed publishers, real HTTP/curl
Range and headers, short expiry, and credential lifetime/rotation. `BlobS3IT` in
the optional reference module adds actual PostgreSQL, Tika, native saves,
independent SQL oracles, rollback/audit failure, retention and GC recovery.

These are server-local source gates. External-machine reachability, generated
consumers, registry artifacts, full module assembly and the remaining R3–R8 gates
require separate evidence. Direct upload and context-checked reads remain false;
a bearer URL can be reused within its valid lifetime without rechecking actor
authorization.
