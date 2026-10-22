package io.teaql.tools.http;

import io.teaql.core.UserContext;
import io.teaql.core.http.HttpObservation;
import io.teaql.core.http.HttpPropagation;
import io.teaql.core.http.HttpRequestFailure;
import io.teaql.core.http.HttpRequestService;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Shared JDK transport for startup registered HTTP profiles. */
public final class JdkHttpRequestService implements HttpRequestService {
    private static final Logger LOG = Logger.getLogger(JdkHttpRequestService.class.getName());
    private final Map<String, State> profiles;
    private final HttpOperationalClock clock;

    public JdkHttpRequestService(List<HttpServiceProfile> profiles) {
        this(profiles, HttpOperationalClock.SYSTEM);
    }

    public JdkHttpRequestService(List<HttpServiceProfile> profiles, HttpOperationalClock clock) {
        Objects.requireNonNull(profiles);
        this.clock = Objects.requireNonNull(clock);
        Map<String, State> states = new LinkedHashMap<>();
        for (HttpServiceProfile profile : profiles) {
            Objects.requireNonNull(profile);
            if (states.putIfAbsent(profile.alias(), new State(profile, clock)) != null) {
                throw new IllegalArgumentException("duplicate HTTP service alias");
            }
        }
        this.profiles = Map.copyOf(states);
    }

    @Override
    public Response execute(UserContext context, Call call) {
        if (context == null || call == null) throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
        State state = profiles.get(call.profile());
        if (state == null) throw new HttpRequestFailure(HttpRequestFailure.Code.PROFILE_NOT_CONFIGURED);
        URI target = safeTarget(state.profile.baseUri(), call.relativePath());
        long started = clock.nanoTime();
        long totalNanos = state.profile.totalTimeout().toNanos();
        int maxAttempts = call.retryMode() == RetryMode.CONFIRMED_IDEMPOTENT
                && state.profile.retryableMethods().contains(call.method())
                ? state.profile.maxAttempts() : 1;
        int attempts = 0;
        String outcome = "unknown";
        HttpObservation.Scope observation = null;
        try {
            HttpObservation observer = context.capability(HttpObservation.class);
            if (observer != null) observation = observer.start(call.observation());
        } catch (RuntimeException ignored) {
            // Observability must not change a business HTTP call's result.
        }
        try {
            // The HTTP span must be active when the runtime injects its carrier.
            Map<String, String> headers = safeHeaders(context, call);
            byte[] body = call.body();
            for (;;) {
                long remaining = remaining(started, totalNanos);
                state.acquireRate(remaining);
                boolean probe = state.acquireCircuit();
                boolean attemptStarted = false;
                boolean settled = false;
                try {
                    remaining = remaining(started, totalNanos);
                    HttpRequest request = buildRequest(target, call, headers, body,
                            Duration.ofNanos(Math.min(remaining, state.profile.requestTimeout().toNanos())));
                    attempts++;
                    attemptStarted = true;
                    CompletableFuture<HttpResponse<byte[]>> future = state.client.sendAsync(
                            request, ignored -> new BoundedSubscriber(state.profile.maxResponseBytes()));
                    HttpResponse<byte[]> response;
                    try {
                        response = future.get(remaining(started, totalNanos), TimeUnit.NANOSECONDS);
                    } catch (InterruptedException interrupted) {
                        future.cancel(true);
                        Thread.currentThread().interrupt();
                        throw new HttpRequestFailure(HttpRequestFailure.Code.CANCELLED);
                    } catch (TimeoutException timeout) {
                        future.cancel(true);
                        throw new HttpRequestFailure(HttpRequestFailure.Code.TIMEOUT);
                    } catch (ExecutionException failed) {
                        future.cancel(true);
                        Throwable cause = failed.getCause();
                        if (hasOverflow(cause)) {
                            throw new HttpRequestFailure(HttpRequestFailure.Code.RESPONSE_TOO_LARGE);
                        }
                        throw new HttpRequestFailure(cause instanceof HttpTimeoutException
                                ? HttpRequestFailure.Code.TIMEOUT
                                : call.method() == Method.POST && maxAttempts == 1
                                ? HttpRequestFailure.Code.AMBIGUOUS_OUTCOME
                                : HttpRequestFailure.Code.UPSTREAM_FAILURE);
                    }
                    if (response.statusCode() == 429 || response.statusCode() >= 500) {
                        state.failed(probe);
                        settled = true;
                        if (attempts < maxAttempts) {
                            backoff(state.profile.retryDelay(), attempts, started, totalNanos);
                            continue;
                        }
                        outcome = "upstream_status";
                    } else {
                        state.succeeded(probe);
                        settled = true;
                        outcome = "success";
                    }
                    return new Response(response.statusCode(), response.body(), attempts);
                } catch (HttpRequestFailure failure) {
                    if (!settled) {
                        if (!attemptStarted || failure.code() == HttpRequestFailure.Code.CANCELLED
                                || failure.code() == HttpRequestFailure.Code.RESPONSE_TOO_LARGE
                                || failure.code() == HttpRequestFailure.Code.INVALID_INPUT) {
                            state.neutral(probe);
                        } else {
                            state.failed(probe);
                        }
                    }
                    if (!settled && attemptStarted && attempts < maxAttempts
                            && failure.code() != HttpRequestFailure.Code.CANCELLED
                            && failure.code() != HttpRequestFailure.Code.RESPONSE_TOO_LARGE
                            && failure.code() != HttpRequestFailure.Code.INVALID_INPUT) {
                        backoff(state.profile.retryDelay(), attempts, started, totalNanos);
                        continue;
                    }
                    outcome = failure.code().name();
                    throw failure;
                } catch (IllegalArgumentException invalid) {
                    if (!settled) state.neutral(probe);
                    throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
                } catch (RuntimeException failed) {
                    if (!settled) state.failed(probe);
                    throw new HttpRequestFailure(HttpRequestFailure.Code.UPSTREAM_FAILURE);
                }
            }
        } catch (HttpRequestFailure failure) {
            // A started POST can have committed upstream even when only the
            // response times out or the connection fails. Before any attempt,
            // keep the ordinary timeout/rate-limit classification.
            HttpRequestFailure reported = attempts > 0 && call.method() == Method.POST
                    && (failure.code() == HttpRequestFailure.Code.TIMEOUT
                            || failure.code() == HttpRequestFailure.Code.UPSTREAM_FAILURE)
                    ? new HttpRequestFailure(HttpRequestFailure.Code.AMBIGUOUS_OUTCOME)
                    : failure;
            outcome = reported.code().name();
            throw reported;
        } finally {
            int count = attempts;
            String safeOutcome = outcome;
            long elapsed = Math.max(0, clock.nanoTime() - started) / 1_000_000;
            if (observation != null) {
                try { observation.finish(safeOutcome, count, elapsed); }
                catch (RuntimeException ignored) { /* Keep the transport result. */ }
            }
            LOG.info(() -> "HTTP alias=" + state.profile.alias() + " method=" + call.method()
                    + " intent=" + call.intentKind() + " comment=" + clean(call.comment())
                    + " purpose=" + clean(call.intent()) + " attempts=" + count
                    + " outcome=" + safeOutcome + " elapsed_ms=" + elapsed);
        }
    }

    private static String clean(String value) {
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    private static boolean hasOverflow(Throwable failure) {
        for (int depth = 0; failure != null && depth < 5; depth++, failure = failure.getCause()) {
            if (failure instanceof ResponseOverflow) return true;
        }
        return false;
    }

    private long remaining(long started, long totalNanos) {
        long left = totalNanos - (clock.nanoTime() - started);
        if (left <= 0) throw new HttpRequestFailure(HttpRequestFailure.Code.TIMEOUT);
        if (Thread.currentThread().isInterrupted()) throw new HttpRequestFailure(HttpRequestFailure.Code.CANCELLED);
        return left;
    }

    private void backoff(Duration initial, int attempts, long started, long totalNanos) {
        long delay = Math.min(initial.toNanos() << Math.min(attempts - 1, 4), remaining(started, totalNanos));
        clock.pauseNanos(delay);
        remaining(started, totalNanos);
    }

    private static URI safeTarget(URI base, String relative) {
        try {
            if (relative.startsWith("/") || relative.indexOf('\\') >= 0) throw new IllegalArgumentException();
            URI part = URI.create(relative);
            if (part.isAbsolute() || part.getRawAuthority() != null || part.getRawFragment() != null
                    || part.getRawPath() == null || part.getRawPath().isEmpty()
                    || part.getRawPath().contains("%")) throw new IllegalArgumentException();
            for (String segment : part.getRawPath().split("/")) {
                if (segment.equals(".") || segment.equals("..")) throw new IllegalArgumentException();
            }
            URI resolved = base.resolve(part).normalize();
            if (!Objects.equals(base.getScheme(), resolved.getScheme())
                    || !Objects.equals(base.getAuthority(), resolved.getAuthority())
                    || !resolved.getPath().startsWith(base.getPath())) throw new IllegalArgumentException();
            return resolved;
        } catch (RuntimeException invalid) {
            throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
        }
    }

    private static Map<String, String> safeHeaders(UserContext context, Call call) {
        Map<String, String> headers = new LinkedHashMap<>(call.headers());
        HttpPropagation propagation = context.capability(HttpPropagation.class);
        if (propagation != null) {
            Map<String, String> carrier = new LinkedHashMap<>();
            try { propagation.inject(carrier); } catch (RuntimeException ignored) { carrier.clear(); }
            for (String name : List.of("traceparent", "tracestate", "baggage")) {
                if (carrier.containsKey(name) && headers.keySet().stream().noneMatch(name::equalsIgnoreCase)) {
                    headers.put(name, carrier.get(name));
                }
            }
        }
        headers.forEach((name, value) -> {
            if (name == null || !name.matches("[A-Za-z0-9!#$%&'*+.^_`|~-]+")
                    || name.equalsIgnoreCase("host") || name.equalsIgnoreCase("connection")
                    || name.equalsIgnoreCase("content-length") || name.equalsIgnoreCase("transfer-encoding")
                    || value == null || value.chars().anyMatch(c -> c < 32 || c == 127)) {
                throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
            }
        });
        return headers;
    }

    private static HttpRequest buildRequest(URI target, Call call, Map<String, String> headers,
                                            byte[] body, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(target).timeout(timeout);
        headers.forEach(builder::header);
        if (call.contentType() != null) builder.header("Content-Type", call.contentType());
        HttpRequest.BodyPublisher publisher = body.length == 0
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body);
        return builder.method(call.method().name(), publisher).build();
    }

    private static final class State {
        final HttpServiceProfile profile;
        final HttpClient client;
        final HttpOperationalClock clock;
        long openUntil;
        int consecutiveFailures;
        int probes;
        double tokens;
        long refilledAt;
        State(HttpServiceProfile profile, HttpOperationalClock clock) {
            this.profile = profile;
            this.clock = clock;
            this.refilledAt = clock.nanoTime();
            HttpClient.Builder builder = HttpClient.newBuilder()
                    .connectTimeout(profile.connectTimeout())
                    .followRedirects(HttpClient.Redirect.NEVER);
            if (!profile.trustedRoots().isEmpty()) builder.sslContext(trustedRootContext(profile));
            this.client = builder.build();
            this.tokens = profile.ratePerSecond();
        }
        private static SSLContext trustedRootContext(HttpServiceProfile profile) {
            try {
                KeyStore store = KeyStore.getInstance("PKCS12");
                store.load(null, null);
                for (int index = 0; index < profile.trustedRoots().size(); index++) {
                    store.setCertificateEntry("root-" + index, profile.trustedRoots().get(index));
                }
                TrustManagerFactory managers = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                managers.init(store);
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, managers.getTrustManagers(), null);
                return context;
            } catch (GeneralSecurityException | java.io.IOException failure) {
                throw new IllegalArgumentException("invalid trusted TLS root");
            }
        }
        synchronized boolean acquireCircuit() {
            if (profile.circuitFailures() == 0) return false;
            if (openUntil == 0) return false;
            if (clock.nanoTime() - openUntil < 0) throw new HttpRequestFailure(HttpRequestFailure.Code.CIRCUIT_OPEN);
            if (probes >= profile.halfOpenProbes()) throw new HttpRequestFailure(HttpRequestFailure.Code.CIRCUIT_OPEN);
            probes++;
            return true;
        }
        synchronized void succeeded(boolean probe) {
            if (probe) probes--;
            consecutiveFailures = 0;
            openUntil = 0;
        }
        synchronized void neutral(boolean probe) { if (probe) probes--; }
        synchronized void failed(boolean probe) {
            if (probe) probes--;
            if (profile.circuitFailures() == 0) return;
            if (probe || ++consecutiveFailures >= profile.circuitFailures()) {
                openUntil = clock.nanoTime() + profile.circuitOpen().toNanos();
                consecutiveFailures = 0;
            }
        }
        void acquireRate(long budget) {
            if (profile.ratePerSecond() == 0) return;
            long started = clock.nanoTime();
            for (;;) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new HttpRequestFailure(HttpRequestFailure.Code.CANCELLED);
                }
                long wait;
                synchronized (this) {
                    long now = clock.nanoTime();
                    tokens = Math.min(profile.ratePerSecond(), tokens
                            + Math.max(0, now - refilledAt) * (double) profile.ratePerSecond() / 1_000_000_000d);
                    refilledAt = now;
                    if (tokens >= 1) { tokens -= 1; return; }
                    wait = Math.max(1, (long) ((1 - tokens) * 1_000_000_000d / profile.ratePerSecond()));
                }
                if (Thread.currentThread().isInterrupted()) throw new HttpRequestFailure(HttpRequestFailure.Code.CANCELLED);
                if (budget - (clock.nanoTime() - started) <= wait) {
                    throw new HttpRequestFailure(HttpRequestFailure.Code.RATE_LIMITED);
                }
                clock.pauseNanos(wait);
            }
        }
    }

    private static final class ResponseOverflow extends RuntimeException {}
    private static final class BoundedSubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final int limit;
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private Flow.Subscription subscription;
        BoundedSubscriber(int limit) { this.limit = limit; }
        @Override public CompletionStage<byte[]> getBody() { return result; }
        @Override public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }
        @Override public void onNext(List<ByteBuffer> chunks) {
            for (ByteBuffer chunk : chunks) {
                if (chunk.remaining() > limit - bytes.size()) {
                    subscription.cancel();
                    result.completeExceptionally(new ResponseOverflow());
                    return;
                }
                byte[] part = new byte[chunk.remaining()];
                chunk.get(part);
                bytes.write(part, 0, part.length);
            }
            subscription.request(1);
        }
        @Override public void onError(Throwable failure) { result.completeExceptionally(failure); }
        @Override public void onComplete() { result.complete(bytes.toByteArray()); }
    }
}
