package io.teaql.storage.s3;

import com.sun.net.httpserver.HttpServer;
import io.teaql.core.http.*;
import io.teaql.core.storage.*;
import io.teaql.runtime.DefaultUserContext;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.interceptor.*;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class SignedSdkCoexistenceTest {
    record Snapshot(String method, String target, Map<String, String> headers) {}

    @Test public void signedSdkTransportDoesNotUseContextHttpOrChangeSignedFields() throws Exception {
        var signed = new CopyOnWriteArrayList<Snapshot>();
        var received = new CopyOnWriteArrayList<Snapshot>();
        var status = new AtomicInteger(200);
        var runtimeCalls = new AtomicInteger();
        HttpRequestService trap = (context, call) -> {
            runtimeCalls.incrementAndGet();
            throw new HttpRequestFailure(HttpRequestFailure.Code.PROFILE_NOT_CONFIGURED);
        };
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            var headers = new TreeMap<String, String>();
            exchange.getRequestHeaders().forEach((key, values) -> headers.put(key.toLowerCase(Locale.ROOT), String.join(",", values)));
            received.add(new Snapshot(exchange.getRequestMethod(), exchange.getRequestURI().toString(), headers));
            exchange.getResponseHeaders().set("Content-Length", "0");
            exchange.getResponseHeaders().set("ETag", "\"sdk-proof-etag\"");
            exchange.sendResponseHeaders(status.get(), -1);
            exchange.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create("sdk-proof-access", "sdk-proof-secret"));
        var configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        var capture = new ExecutionInterceptor() {
            @Override public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes attributes) {
                var request = context.httpRequest();
                var headers = new TreeMap<String, String>();
                request.headers().forEach((key, values) -> headers.put(key.toLowerCase(Locale.ROOT), String.join(",", values)));
                headers.putIfAbsent("host", request.getUri().getRawAuthority());
                String target = request.getUri().getRawPath();
                if (request.getUri().getRawQuery() != null) target += "?" + request.getUri().getRawQuery();
                signed.add(new Snapshot(request.method().name(), target, headers));
            }
        };
        try (var provider = new S3ObjectStorage(S3Client.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                .credentialsProvider(credentials).serviceConfiguration(configuration)
                .overrideConfiguration(b -> b.retryPolicy(RetryPolicy.none()).apiCallTimeout(Duration.ofSeconds(3)).addExecutionInterceptor(capture)).build(),
                S3Presigner.builder().endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(credentials).serviceConfiguration(configuration).build(), "bucket")) {
            var context = new DefaultUserContext(null);
            context.putAttribute(ObjectStorageProvider.class.getName(), provider);
            context.putAttribute(HttpRequestService.class.getName(), trap);
            context.putAttribute(HttpPropagation.class.getName(), (HttpPropagation) carrier -> carrier.put("traceparent", "runtime-http-must-not-inject-this"));
            assertSame(trap, context.httpRequests());
            assertThrows(HttpRequestFailure.class, () -> context.httpRequests().execute(context,
                    HttpRequestService.Call.get("trap", "control", "check SPI installation", "verify SDK isolation")));
            assertEquals(1, runtimeCalls.get());
            var storage = context.objectStorage("verify signed SDK isolation");
            var key = new ObjectStorage.Key("sdk-proof");
            assertTrue(storage.exists(key));
            status.set(503);
            assertEquals(StorageException.Code.UNAVAILABLE, assertThrows(StorageException.class, () -> storage.exists(key)).code());
            assertEquals("runtime HTTP must not wrap or retry either SDK call", 1, runtimeCalls.get());
            assertEquals(2, signed.size()); assertEquals(2, received.size());
            for (int i = 0; i < 2; i++) {
                var before = signed.get(i); var wire = received.get(i);
                assertEquals(before.method(), wire.method()); assertEquals(before.target(), wire.target());
                String authorization = before.headers().get("authorization");
                assertNotNull(authorization); assertTrue(authorization.startsWith("AWS4-HMAC-SHA256 "));
                assertTrue("authorization changed after SDK signing", authorization.equals(wire.headers().get("authorization")));
                String names = authorization.split("SignedHeaders=", 2)[1].split(",", 2)[0];
                for (String name : names.split(";")) assertTrue("SDK signed header changed: " + name,
                        Objects.equals(before.headers().get(name), wire.headers().get(name)));
                assertFalse(wire.headers().containsKey("traceparent"));
            }
            System.out.println("HTTP_SDK_COEXISTENCE_PASS language=java control_calls=1 runtime_sdk_calls=0 sdk_attempts=2 wire_requests=2 signatures=2");
        } finally { server.stop(0); }
    }
}
