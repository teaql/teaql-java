# Context owned object storage example

Tracking: [Java #211](https://github.com/teaql/teaql-java/issues/211),
[shared contract #122](https://github.com/teaql/teaql-conformance/issues/122).

The low-level runtime facade stores binary objects through an optional S3
provider. It requires no generated domain model. All TeaQL dependencies in
this example resolve from this repository's Maven reactor.

```java
context.putAttribute(ObjectStorageProvider.class.getName(), provider);
var storage = context.objectStorage("store payment receipt attachment");
var key = new ObjectStorage.Key("receipts/opaque-reference.pdf");
storage.put(key, inputStream, ObjectStorage.PutOptions.create(contentLength));
var link = storage.getReadUrl(key, ObjectStorage.ReadUrlOptions.defaults());
// Return link.url() to the authorized caller; never log the signed URL.
try (var content = storage.get(key)) {
    content.stream().transferTo(destination);
}
```

Trusted startup constructs one `S3ObjectStorage` with shared SDK clients and
installs it on each context. `S3ObjectStorage.connect` accepts an endpoint
(null for AWS), region, an SDK credentials provider, bucket and path-style flag.
The SDK credentials provider can use the default chain or session credentials.
The provider owns its client/presigner; close it at application shutdown, not
after every operation. The default factory uses the SDK Apache 5 HTTP client
with an owned connection pool. Runtime telemetry observes operations automatically.

Keys are durable; URLs default to five minutes and are reusable bearer links.
Expiration is an upper bound, subject to credential expiry and provider policy.
Only the application decides who may obtain a URL. A signed URL does not prove
that the object exists. Keys, custom metadata and URLs are masked in diagnostic
string representations; call explicit accessors only when actually needed.

Uploads spool to a private temporary file with bounded memory before a retryable
single PUT. Declared length must match exactly; the limit is 5 GiB. The facade
closes the supplied stream. Downloads are direct streams and must be closed.
`getBytes(key, maximumBytes)` enforces a size limit. Create-only uses atomic
`If-None-Match`; replace must be explicit. SQL transactions do not include S3.

## Verify against a dedicated local S3 service

Configure `TEAQL_S3_ENDPOINT`, `TEAQL_S3_BUCKET`, `TEAQL_S3_ACCESS_KEY` and
`TEAQL_S3_SECRET_KEY` for a test-only bucket. The example creates that bucket if
absent; the production provider never creates buckets. Each run uses unique
keys and deletes only those keys. Do not point it at production storage.

```bash
bash examples/object-storage/verify.sh
bash examples/object-storage/verify.sh
# Optional repeated lifecycle probe in one JVM:
TEAQL_S3_ITERATIONS=10 bash examples/object-storage/verify.sh
```

The script fails if configuration is absent. It runs the core contract and
HTTP error tests and enables the live example explicitly. It tests binary/empty
data, metadata, duplicate and concurrent create-only, explicit replacement,
bounded reads, signed URL download and expiry, missing bucket and repeated
delete. Plain `mvn test` excludes live execution unless `-Dteaql.s3.live=true`;
that exclusion is not live-provider evidence.

The provider SDK version is pinned in `teaql-object-storage-s3/pom.xml`.
AWS-hosted S3 and other compatible providers require separate qualification.
