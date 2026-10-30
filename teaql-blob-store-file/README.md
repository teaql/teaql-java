# Filesystem Blob transport

Tracking [Java #272](https://github.com/teaql/teaql-java/issues/272) and
[conformance #237](https://github.com/teaql/teaql-conformance/issues/237).

This optional Java 17 artifact implements the core `BlobStore` transport SPI.
It depends on core, the private file staging helper and the JDK HTTP server,
without a SQL, Spring, runtime or
secure-document implementation dependency. The base runtime never installs it
automatically. Constructing the store/gateway does no I/O or background work.

`FileBlobStore.initialize()` explicitly provisions a dedicated private POSIX root.
An existing unrelated or symlink root is rejected. Upload locations derive from
trusted upload identifiers; filenames never choose filesystem paths. Each object
has a fixed 64-byte header containing its format, random immutable generation,
payload length and SHA-256, followed by the exact uploaded bytes. Writes are bounded,
fsynced and atomically published; unsupported operations fail without a weaker
publication fallback. Identical upload retries keep the existing generation;
different bytes conflict. Partial stages are removed on ordinary failure.
Each upload now owns a leased private scratch job; an explicit global host
`stagingMaintenance(permission)` can reap abandoned jobs after process termination.
Active jobs remain protected by OS leases even if their timestamps are old. This
authority is separate from tenant catalog collection; it never deletes published
objects. See `teaql-blob-staging-file` for limits/cursors and legacy drain rules.

A fixed set of JVM locks plus OS file locks coordinates all instances/processes
using a root. Version-conditional deletion compares and unlinks under that same
exclusive lock. Missing objects acknowledge an already completed delete; a version
mismatch never falls back to deletion. Open range streams retain their original
immutable inode even when a later generation replaces the path after deletion.
All returned streams are caller-owned; the caller retains ownership of write input.

`FileBlobHttpGateway` is an optional host-owned wrapper around this store.
`start()`/`close()` explicitly manage only its listener and bounded worker pool.
The host configures its complete public endpoint URI and a versioned signing-key
map independently of document keys. A configured loopback HTTP endpoint with port
0 resolves to the listener's actual ephemeral port; deployed HTTPS origins may be
served through the host's TLS proxy. `signReadBatch` returns complete untouched URLs
and actual expiries capped by the store limit. Download claims bind location,
content version, response headers, public origin and expiry. Signing keys are copied.

The gateway supports GET/HEAD and single byte ranges, including suffix/open-ended
ranges. It serves actual content bytes, safe UTF-8 Content-Disposition, verified
length, private cache policy and nosniff. Cache max-age is also capped by the URL's
remaining lifetime on each read. Expired/tampered credentials reject before content
access; an old URL cannot read a subsequently created generation. URL expiration
does not collect the stored object. Values/errors do not print storage locations,
signing credentials or bearer URLs.

Transport signing is a trusted server operation. Domain scope, current binding,
entity/field permissions and additional Blob policy belong to Blob services above
this SPI. These URLs are transferable bearer capabilities. Direct upload and
context-checked reads are explicitly unsupported by this adapter.

The transport suite has 13 test methods: all ten retained real formats, exact
size/digest/stream bounds, immutable retry, stale-version deletion, path/symlink and
header-corruption controls, concurrent instances, a separate JVM's OS lock, actual
HTTP/Range/HEAD/TTL/cache/token controls, and an independent curl process using the
returned URL. Fixture copies retain the shared conformance manifest/digests.
Test directories, listeners, worker pools and child processes have explicit cleanup.

The optional reference/verifier modules now supply actual format verification,
SQL upload/binding and physical collection, with separate crash/staging gates.
Complete BlobRuntime/BlobReferenceModule assembly, authorized presentation, S3,
external-host clients and generated/release gates remain pending. Transport byte
round trips alone do not establish those service-level acceptance results.
