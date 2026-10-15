package io.teaql.tools.http;

import io.teaql.core.http.HttpRequestService;
import java.net.URI;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Trusted startup configuration for one named upstream service. */
public final class HttpServiceProfile {
    private final String alias;
    private final URI baseUri;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final Duration totalTimeout;
    private final int maxResponseBytes;
    private final int maxAttempts;
    private final Duration retryDelay;
    private final Set<HttpRequestService.Method> retryableMethods;
    private final int circuitFailures;
    private final Duration circuitOpen;
    private final int halfOpenProbes;
    private final int ratePerSecond;

    private HttpServiceProfile(Builder b) {
        this.alias = b.alias;
        this.baseUri = b.baseUri;
        this.connectTimeout = b.connectTimeout;
        this.requestTimeout = b.requestTimeout;
        this.totalTimeout = b.totalTimeout;
        this.maxResponseBytes = b.maxResponseBytes;
        this.maxAttempts = b.maxAttempts;
        this.retryDelay = b.retryDelay;
        this.retryableMethods = Set.copyOf(b.retryableMethods);
        this.circuitFailures = b.circuitFailures;
        this.circuitOpen = b.circuitOpen;
        this.halfOpenProbes = b.halfOpenProbes;
        this.ratePerSecond = b.ratePerSecond;
    }

    public static Builder builder(String alias, URI baseUri) { return new Builder(alias, baseUri); }
    public String alias() { return alias; }
    public URI baseUri() { return baseUri; }
    public Duration connectTimeout() { return connectTimeout; }
    public Duration requestTimeout() { return requestTimeout; }
    public Duration totalTimeout() { return totalTimeout; }
    public int maxResponseBytes() { return maxResponseBytes; }
    public int maxAttempts() { return maxAttempts; }
    public Duration retryDelay() { return retryDelay; }
    public Set<HttpRequestService.Method> retryableMethods() { return retryableMethods; }
    public int circuitFailures() { return circuitFailures; }
    public Duration circuitOpen() { return circuitOpen; }
    public int halfOpenProbes() { return halfOpenProbes; }
    public int ratePerSecond() { return ratePerSecond; }
    @Override public String toString() { return "HttpServiceProfile[alias=" + alias + ", endpoint=masked]"; }

    public static final class Builder {
        private final String alias;
        private final URI baseUri;
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration requestTimeout = Duration.ofSeconds(5);
        private Duration totalTimeout = Duration.ofSeconds(10);
        private int maxResponseBytes = 1024 * 1024;
        private int maxAttempts = 1;
        private Duration retryDelay = Duration.ofMillis(100);
        private Set<HttpRequestService.Method> retryableMethods = EnumSet.noneOf(HttpRequestService.Method.class);
        private int circuitFailures;
        private Duration circuitOpen = Duration.ofSeconds(10);
        private int halfOpenProbes = 1;
        private int ratePerSecond;

        private Builder(String alias, URI baseUri) {
            if (alias == null || !alias.matches("[A-Za-z][A-Za-z0-9_-]{0,63}")) {
                throw new IllegalArgumentException("invalid HTTP service alias");
            }
            URI uri = Objects.requireNonNull(baseUri).normalize();
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || (uri.getRawPath() != null && uri.getRawPath().contains("%"))) {
                throw new IllegalArgumentException("invalid HTTP service base URI");
            }
            String path = uri.getPath() == null ? "/" : uri.getPath();
            if (!path.endsWith("/")) path += "/";
            this.alias = alias;
            this.baseUri = uri.resolve(path);
        }

        public Builder budgets(Duration connect, Duration request, Duration total) {
            this.connectTimeout = positive(connect);
            this.requestTimeout = positive(request);
            this.totalTimeout = positive(total);
            return this;
        }
        public Builder maxResponseBytes(int bytes) { this.maxResponseBytes = bytes; return this; }
        public Builder retry(int attempts, Duration delay, Set<HttpRequestService.Method> methods) {
            this.maxAttempts = attempts;
            this.retryDelay = positive(delay);
            Set<HttpRequestService.Method> selected = Objects.requireNonNull(methods);
            this.retryableMethods = selected.isEmpty()
                    ? EnumSet.noneOf(HttpRequestService.Method.class)
                    : EnumSet.copyOf(selected);
            return this;
        }
        public Builder circuit(int failures, Duration open, int halfOpenProbes) {
            this.circuitFailures = failures;
            this.circuitOpen = positive(open);
            this.halfOpenProbes = halfOpenProbes;
            return this;
        }
        public Builder ratePerSecond(int rate) { this.ratePerSecond = rate; return this; }
        public HttpServiceProfile build() {
            if (maxResponseBytes < 1 || maxResponseBytes > 16 * 1024 * 1024
                    || maxAttempts < 1 || maxAttempts > 5 || circuitFailures < 0
                    || halfOpenProbes < 1 || ratePerSecond < 0) {
                throw new IllegalArgumentException("invalid HTTP service limits");
            }
            return new HttpServiceProfile(this);
        }
        private static Duration positive(Duration value) {
            if (value == null || value.isZero() || value.isNegative()
                    || value.compareTo(Duration.ofHours(1)) > 0) {
                throw new IllegalArgumentException("HTTP duration must be positive and finite");
            }
            return value;
        }
    }
}
