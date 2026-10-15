# TeaQL S3 object storage provider

Implements the core `ObjectStorageProvider` SPI using the official AWS SDK v2.
Use it through `UserContext.objectStorage(comment)`; configure a provider at
trusted startup. See the [local runtime example](../examples/object-storage/README.md)
for the six operations, stream lifecycle, limitations and live verification.

No bucket administration, multipart API, domain authorization or cross-store
transaction is implied. SDK exceptions are translated into sanitized storage
categories. HEAD 403/5xx never become absence; a HEAD 404 checks the bucket
before declaring the object missing.
