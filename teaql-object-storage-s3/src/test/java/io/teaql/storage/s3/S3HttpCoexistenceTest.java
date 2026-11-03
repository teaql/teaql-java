package io.teaql.storage.s3;

import com.sun.net.httpserver.HttpServer;
import io.teaql.core.UserContext;
import io.teaql.core.http.HttpPropagation;
import io.teaql.core.http.HttpRequestService;
import io.teaql.core.storage.ObjectStorage;
import io.teaql.core.storage.ObjectStorageProvider;
import io.teaql.core.storage.StorageException;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.tools.http.HttpServiceProfile;
import io.teaql.tools.http.JdkHttpRequestService;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** The real SDK keeps its own signer, transport and retry policy. */
public class S3HttpCoexistenceTest {
    @Test public void signedSdkRequestIsNotWrappedRewrittenOrRetriedByRuntimeHttp() throws Exception {
        var sdkRequests = new AtomicInteger(); var runtimeRequests = new AtomicInteger();
        var signedAuthorization = new AtomicReference<String>(); var wireAuthorization = new AtomicReference<String>();
        var sdkTrace = new AtomicReference<String>(); var runtimeTrace = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", request -> {
            request.getRequestBody().readAllBytes();
            if (request.getRequestMethod().equals("HEAD")) {
                sdkRequests.incrementAndGet(); wireAuthorization.set(request.getRequestHeaders().getFirst("Authorization"));
                sdkTrace.set(request.getRequestHeaders().getFirst("traceparent")); request.sendResponseHeaders(503, -1);
            } else {
                runtimeRequests.incrementAndGet(); runtimeTrace.set(request.getRequestHeaders().getFirst("traceparent"));
                request.sendResponseHeaders(200, 0);
            }
            request.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create("SDK_ACCESS_KEY_CANARY", "SDK_SECRET_CANARY"));
        var configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        ExecutionInterceptor signed = new ExecutionInterceptor() {
            @Override public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes attributes) {
                signedAuthorization.set(context.httpRequest().firstMatchingHeader("Authorization").orElse(null));
            }
        };
        try (var provider = new S3ObjectStorage(S3Client.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                .credentialsProvider(credentials).serviceConfiguration(configuration)
                .overrideConfiguration(b -> b.retryPolicy(RetryPolicy.none()).apiCallTimeout(Duration.ofSeconds(3))
                        .addExecutionInterceptor(signed)).build(),
                S3Presigner.builder().endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(credentials)
                        .serviceConfiguration(configuration).build(), "bucket")) {
            var profile = HttpServiceProfile.builder("orders", endpoint.resolve("/api/"))
                    .retry(3, Duration.ofMillis(1), Set.of(HttpRequestService.Method.GET)).build();
            var calls = new AtomicInteger(); var delegate = new JdkHttpRequestService(List.of(profile));
            HttpRequestService spy = (UserContext context, HttpRequestService.Call call) -> {
                calls.incrementAndGet(); return delegate.execute(context, call);
            };
            var context = new DefaultUserContext(null);
            context.putAttribute(HttpRequestService.class.getName(), spy);
            context.putAttribute(ObjectStorageProvider.class.getName(), provider);
            String trace = "00-11111111111111111111111111111111-aaaaaaaaaaaaaaaa-01";
            context.putAttribute(HttpPropagation.class.getName(), (HttpPropagation) carrier -> carrier.put("traceparent", trace));
            var error = assertThrows(StorageException.class,
                    () -> context.objectStorage("verify SDK transport isolation").exists(new ObjectStorage.Key("SDK_OBJECT_CANARY")));
            assertEquals(StorageException.Code.UNAVAILABLE, error.code()); assertNull(error.getCause());
            assertEquals(1, sdkRequests.get()); assertEquals(0, calls.get()); assertEquals(0, runtimeRequests.get());
            assertNotNull(signedAuthorization.get()); assertTrue(signedAuthorization.get().startsWith("AWS4-HMAC-SHA256 "));
            assertTrue(signedAuthorization.get().contains("Credential=SDK_ACCESS_KEY_CANARY/"));
            assertTrue(signedAuthorization.get().contains("Signature=")); assertEquals(signedAuthorization.get(), wireAuthorization.get());
            assertNull(sdkTrace.get()); assertFalse(error.toString().contains("CANARY"));
            assertEquals(200, context.httpRequests().execute(context, HttpRequestService.Call.get("orders", "status",
                    "check runtime HTTP control", "prove the independent runtime transport remains usable")).status());
            assertEquals(1, calls.get()); assertEquals(1, runtimeRequests.get()); assertEquals(trace, runtimeTrace.get());
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            System.out.println("JAVA_HTTP_CONTRACT_CASE=" + mapper.writeValueAsString(Map.of("id", "HTTP-15",
                    "realAwsSdk", true, "sdkPhysicalRequests", 1, "runtimeCallsDuringSdk", 0, "runtimeControlCalls", 1,
                    "signedAuthorizationPreserved", true, "sdkTraceNotInjected", true)));
            System.out.println("JAVA_HTTP_SOURCE_ORIGINS=" + mapper.writeValueAsString(Map.of(
                    "core", UserContext.class.getProtectionDomain().getCodeSource().getLocation().toString(),
                    "runtime", DefaultUserContext.class.getProtectionDomain().getCodeSource().getLocation().toString(),
                    "http", JdkHttpRequestService.class.getProtectionDomain().getCodeSource().getLocation().toString(),
                    "s3", S3ObjectStorage.class.getProtectionDomain().getCodeSource().getLocation().toString(),
                    "aws", S3Client.class.getProtectionDomain().getCodeSource().getLocation().toString())));
        } finally { server.stop(0); }
    }
}
