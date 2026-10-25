package io.teaql.blob.store.s3;

import java.net.URI;
import java.util.*;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import static org.junit.Assert.*;

/** Explicit real-backend preflight. No Assume/skip and no acceptance from mocked SDK responses. */
public class S3BackendIT {
    @Test public void conditionalCreationVersionIdentityAndSpecificDeletionActuallyHold() throws Exception {
        try (Fixture f=new Fixture()) {
            String key=f.prefix+"object",other=f.prefix+"other"; byte[] bytes={1,2,3};
            var first=f.client.putObject(b -> b.bucket(f.bucket).key(key).ifNoneMatch("*"),RequestBody.fromBytes(bytes));
            assertNotNull(first.versionId()); assertFalse(first.versionId().isBlank()); assertNotEquals("null",first.versionId());
            assertEquals(412,assertThrows(S3Exception.class,() -> f.client.putObject(b -> b.bucket(f.bucket).key(key).ifNoneMatch("*"),RequestBody.fromBytes(new byte[]{4}))).statusCode());
            var second=f.client.putObject(b -> b.bucket(f.bucket).key(key),RequestBody.fromBytes(bytes));
            assertNotEquals(first.versionId(),second.versionId()); assertEquals(first.eTag(),second.eTag());
            assertArrayEquals(bytes,f.client.getObjectAsBytes(b -> b.bucket(f.bucket).key(key).versionId(first.versionId())).asByteArray());
            var unrelated=f.client.putObject(b -> b.bucket(f.bucket).key(other),RequestBody.fromBytes(bytes));
            try { f.client.deleteObject(b -> b.bucket(f.bucket).key(key).versionId(unrelated.versionId())); }
            catch (S3Exception absent) { assertEquals(404,absent.statusCode()); }
            assertEquals(second.versionId(),f.client.headObject(b -> b.bucket(f.bucket).key(key)).versionId());
            f.client.deleteObject(b -> b.bucket(f.bucket).key(key).versionId(first.versionId()));
            assertEquals(404,assertThrows(S3Exception.class,() -> f.client.getObjectAsBytes(b -> b.bucket(f.bucket).key(key).versionId(first.versionId()))).statusCode());
            assertEquals(second.versionId(),f.client.headObject(b -> b.bucket(f.bucket).key(key)).versionId());
        }
    }
    @Test public void actualBackendRejectsFalseDeleteEtagWithoutRemovingTheObject() throws Exception {
        try (Fixture f=new Fixture()) {
            String key=f.prefix+"conditional-delete"; byte[] bytes={4,5,6};
            var first=f.client.putObject(b -> b.bucket(f.bucket).key(key),RequestBody.fromBytes(bytes));
            assertEquals(412,assertThrows(S3Exception.class,() -> f.client.deleteObject(b -> b.bucket(f.bucket).key(key).ifMatch("\"not-the-object-etag\""))).statusCode());
            assertEquals(first.versionId(),f.client.headObject(b -> b.bucket(f.bucket).key(key)).versionId());
            assertArrayEquals(bytes,f.client.getObjectAsBytes(b -> b.bucket(f.bucket).key(key).versionId(first.versionId())).asByteArray());
            f.client.deleteObject(b -> b.bucket(f.bucket).key(key).versionId(first.versionId()).ifMatch(first.eTag()));
            assertEquals(404,assertThrows(S3Exception.class,() -> f.client.headObject(b -> b.bucket(f.bucket).key(key))).statusCode());
        }
    }
    static final class Fixture implements AutoCloseable {
        final String bucket=required("TEAQL_BLOB_S3_BUCKET");
        final String prefix="backend-preflight/"+UUID.randomUUID()+"/";
        final S3Client client;
        Fixture() {
            client=S3Client.builder().endpointOverride(URI.create(required("TEAQL_BLOB_S3_ENDPOINT"))).region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(required("TEAQL_BLOB_S3_ACCESS_KEY"),required("TEAQL_BLOB_S3_SECRET_KEY"))))
                    .forcePathStyle(true).httpClientBuilder(Apache5HttpClient.builder().connectionTimeout(java.time.Duration.ofSeconds(5)).socketTimeout(java.time.Duration.ofSeconds(15)))
                    .overrideConfiguration(configuration -> configuration.apiCallTimeout(java.time.Duration.ofSeconds(30))).build();
            try { client.headBucket(b -> b.bucket(bucket)); }
            catch (S3Exception missing) { if (missing.statusCode()!=404) throw missing; client.createBucket(b -> b.bucket(bucket)); }
            // Test provisioning only. The production adapter must never change bucket administration.
            client.putBucketVersioning(b -> b.bucket(bucket).versioningConfiguration(v -> v.status(BucketVersioningStatus.ENABLED)));
            assertEquals(BucketVersioningStatus.ENABLED,client.getBucketVersioning(b -> b.bucket(bucket)).status());
        }
        @Override public void close() {
            try {
                for (var page:client.listObjectVersionsPaginator(b -> b.bucket(bucket).prefix(prefix))) {
                    for (var object:page.versions()) client.deleteObject(b -> b.bucket(bucket).key(object.key()).versionId(object.versionId()));
                    for (var marker:page.deleteMarkers()) client.deleteObject(b -> b.bucket(bucket).key(marker.key()).versionId(marker.versionId()));
                }
            } finally { client.close(); }
        }
    }
    private static String required(String name) { String value=System.getenv(name); if (value==null || value.isBlank()) throw new IllegalStateException("Missing test configuration: "+name); return value; }
}
