package io.teaql.storage.s3;

import io.teaql.core.storage.ObjectStorage;
import io.teaql.core.storage.ObjectStorage.*;
import io.teaql.core.storage.ObjectStorageProvider;
import io.teaql.core.storage.StorageException;
import static io.teaql.core.storage.StorageException.Code.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.exception.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** Owns its SDK client and presigner. No bucket administration or custom signing. */
public final class S3ObjectStorage implements ObjectStorageProvider, AutoCloseable {
    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3ObjectStorage(S3Client client, S3Presigner presigner, String bucket) {
        this.client = Objects.requireNonNull(client);
        this.presigner = Objects.requireNonNull(presigner);
        if (bucket == null || bucket.isBlank()) throw new StorageException(INVALID_INPUT);
        this.bucket = bucket;
    }

    /** Endpoint may be null for AWS. Credentials may use the SDK default/session provider chain. */
    public static S3ObjectStorage connect(URI endpoint, Region region, AwsCredentialsProvider credentials,
            String bucket, boolean pathStyle) {
        var configuration = S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build();
        var client = S3Client.builder().region(region).credentialsProvider(credentials)
                .serviceConfiguration(configuration)
                .httpClientBuilder(Apache5HttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(10)).socketTimeout(Duration.ofSeconds(30)))
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofMinutes(2)));
        var signer = S3Presigner.builder().region(region).credentialsProvider(credentials)
                .serviceConfiguration(configuration);
        if (endpoint != null) { client.endpointOverride(endpoint); signer.endpointOverride(endpoint); }
        return new S3ObjectStorage(client.build(), signer.build(), bucket);
    }

    @Override public Metadata put(Invocation invocation, Key key, InputStream content, PutOptions options) {
        Objects.requireNonNull(invocation);
        Path temporary = null;
        try {
            temporary = Files.createTempFile("teaql-object-", ".upload");
            try (OutputStream output = Files.newOutputStream(temporary)) {
                byte[] buffer = new byte[64 * 1024];
                long copied = 0;
                int count;
                while ((count = content.read(buffer)) != -1) {
                    copied += count;
                    if (copied > options.size()) throw new StorageException(INVALID_INPUT);
                    output.write(buffer, 0, count);
                }
                if (copied != options.size()) throw new StorageException(INVALID_INPUT);
            }
            var request = PutObjectRequest.builder().bucket(bucket).key(key.value())
                    .contentLength(options.size()).contentType(options.contentType()).metadata(options.metadata());
            if (options.mode() == WriteMode.CREATE_ONLY) request.ifNoneMatch("*");
            var result = client.putObject(request.build(), RequestBody.fromFile(temporary));
            if (result.eTag() == null || result.eTag().isBlank()) throw new StorageException(UNAVAILABLE);
            return new Metadata(key, options.size(), options.contentType(), result.eTag(), null, options.metadata());
        } catch (SdkException error) { throw translate(error); }
        catch (IOException error) { throw new StorageException(IO); }
        finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException error) { throw new StorageException(IO); }
            }
        }
    }

    @Override public Content get(Invocation invocation, Key key) {
        Objects.requireNonNull(invocation);
        try {
            var body = client.getObject(GetObjectRequest.builder().bucket(bucket).key(key.value()).build());
            var result = body.response();
            if (result.contentLength() == null || result.contentLength() < 0 || result.eTag() == null || result.eTag().isBlank()) {
                body.abort(); throw new StorageException(UNAVAILABLE);
            }
            return new Content(new Metadata(key, result.contentLength(), result.contentType(), result.eTag(),
                    result.lastModified(), result.metadata()), body);
        } catch (SdkException error) { throw translate(error); }
    }

    @Override public Optional<Metadata> head(Invocation invocation, Key key) {
        Objects.requireNonNull(invocation);
        try {
            var result = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key.value()).build());
            if (result.contentLength() == null || result.contentLength() < 0 || result.eTag() == null || result.eTag().isBlank()) throw new StorageException(UNAVAILABLE);
            return Optional.of(new Metadata(key, result.contentLength(), result.contentType(), result.eTag(),
                    result.lastModified(), result.metadata()));
        } catch (S3Exception error) {
            if (error.statusCode() == 404) {
                // HEAD has no error body: confirm the bucket exists before declaring object absence.
                try { client.headBucket(b -> b.bucket(bucket)); return Optional.empty(); }
                catch (SdkException bucketError) {
                    StorageException translated = translate(bucketError);
                    if (translated.code() == NOT_FOUND) throw new StorageException(UNAVAILABLE);
                    throw translated;
                }
            }
            throw translate(error);
        } catch (SdkException error) { throw translate(error); }
    }

    @Override public void delete(Invocation invocation, Key key) {
        Objects.requireNonNull(invocation);
        try { client.deleteObject(b -> b.bucket(bucket).key(key.value())); }
        catch (SdkException error) { throw translate(error); }
    }

    @Override public boolean supportsVersionConditionalDelete() { return true; }

    /** S3 DeleteObject If-Match; a mismatch remains CONFLICT and is never retried unconditionally. */
    @Override public void deleteIfVersionMatches(Invocation invocation, Key key, String expectedVersion) {
        Objects.requireNonNull(invocation);
        try { client.deleteObject(b -> b.bucket(bucket).key(key.value()).ifMatch(expectedVersion)); }
        catch (SdkException error) { throw translate(error); }
    }

    @Override public ReadUrl getReadUrl(Invocation invocation, Key key, ReadUrlOptions options) {
        Objects.requireNonNull(invocation);
        try {
            var request = GetObjectRequest.builder().bucket(bucket).key(key.value())
                    .responseContentDisposition(options.contentDisposition()).build();
            var signed = presigner.presignGetObject(b -> b.signatureDuration(options.lifetime()).getObjectRequest(request));
            if (!signed.isBrowserExecutable()) throw new StorageException(UNSUPPORTED);
            return new ReadUrl(signed.url().toURI(), signed.expiration());
        } catch (SdkException error) { throw translate(error); }
        catch (java.net.URISyntaxException error) { throw new StorageException(UNAVAILABLE); }
    }

    static StorageException translate(SdkException error) {
        if (error instanceof ApiCallTimeoutException || error instanceof ApiCallAttemptTimeoutException)
            return new StorageException(TIMEOUT);
        Throwable cause = error.getCause();
        for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
            if (cause instanceof java.net.SocketTimeoutException || cause instanceof java.util.concurrent.TimeoutException)
                return new StorageException(TIMEOUT);
        }
        if (error instanceof S3Exception s3) {
            String code = s3.awsErrorDetails() == null ? "" : s3.awsErrorDetails().errorCode();
            if ("NoSuchBucket".equals(code)) return new StorageException(UNAVAILABLE);
            return new StorageException(switch (s3.statusCode()) {
                case 401, 403 -> ACCESS_DENIED;
                case 404 -> NOT_FOUND;
                case 409, 412 -> CONFLICT;
                case 408, 504 -> TIMEOUT;
                case 413 -> TOO_LARGE;
                case 501 -> UNSUPPORTED;
                case 400, 422 -> INVALID_INPUT;
                default -> UNAVAILABLE;
            });
        }
        return new StorageException(UNAVAILABLE);
    }

    @Override public void close() { try { client.close(); } finally { presigner.close(); } }
    @Override public String toString() { return "S3ObjectStorage[configuration=masked]"; }
}
