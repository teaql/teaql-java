package io.teaql.opentelemetry;

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.HttpResponse;
import io.opentelemetry.sdk.common.export.HttpSender;
import io.opentelemetry.sdk.common.export.HttpSenderProvider;
import io.opentelemetry.sdk.common.export.HttpSenderConfig;
import io.opentelemetry.sdk.common.export.MessageWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Instance-local SDK transport boundary: never changes global SDK/JUL settings. */
final class OwnedHttpComponents implements ComponentLoader {
    private final ComponentLoader delegate;
    private final OtlpResponseAccounting responses;

    OwnedHttpComponents(ClassLoader loader, OtlpResponseAccounting responses) {
        delegate = ComponentLoader.forClassLoader(loader); this.responses = responses;
    }

    @Override public <T> Iterable<T> load(Class<T> type) {
        if (type != HttpSenderProvider.class) return delegate.load(type);
        HttpSenderProvider provider = ComponentLoader.loadList(delegate, HttpSenderProvider.class)
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("HTTP sender unavailable"));
        HttpSenderProvider safe = config -> protect(provider.createSender(bounded(config)),
                responses, signal(config.getEndpoint().getPath()));
        return List.of(type.cast(safe));
    }

    static HttpSender protect(HttpSender delegate) {
        return protect(delegate, null, null);
    }

    static HttpSender protect(HttpSender delegate, OtlpResponseAccounting responses, ExportLossAccounting.Signal signal) {
        return new HttpSender() {
            @Override public void send(MessageWriter message, Consumer<HttpResponse> response, Consumer<Throwable> error) {
                AtomicBoolean completed = new AtomicBoolean();
                Consumer<Throwable> safeError = ignored -> {
                    if (completed.compareAndSet(false, true)) error.accept(new SafeTransportFailure());
                };
                try {
                    delegate.send(message, actual -> {
                        int status;
                        try { status = actual.getStatusCode(); }
                        catch (Throwable ignored) { safeError.accept(null); return; }
                        if (!completed.compareAndSet(false, true)) return;
                        // Decode successful acknowledgements before the SDK drops their
                        // partial-success fields. Error bodies/reasons remain unread.
                        if (status >= 200 && status < 300 && responses != null) {
                            boolean valid;
                            try {
                                if (status != 200) { responses.invalid(signal); valid = false; }
                                else valid = responses.receive(signal, actual.getResponseBody());
                            }
                            catch (Throwable ignored) { responses.invalid(signal); valid = false; }
                            if (!valid) { error.accept(new SafeTransportFailure()); return; }
                        }
                        final int code = status >= 100 && status <= 599 ? status : 0;
                        response.accept(new HttpResponse() {
                            @Override public int getStatusCode() { return code; }
                            @Override public String getStatusMessage() { return "remote response masked"; }
                            @Override public byte[] getResponseBody() { return new byte[0]; }
                        });
                    }, safeError);
                } catch (Throwable ignored) { safeError.accept(null); }
            }

            @Override public CompletableResultCode shutdown() {
                try { return delegate.shutdown(); }
                catch (Throwable ignored) { return CompletableResultCode.ofFailure(); }
            }
        };
    }

    private static ExportLossAccounting.Signal signal(String path) {
        if (path.endsWith("/v1/traces")) return ExportLossAccounting.Signal.TRACES;
        if (path.endsWith("/v1/logs")) return ExportLossAccounting.Signal.LOGS;
        if (path.endsWith("/v1/metrics")) return ExportLossAccounting.Signal.METRICS;
        throw new IllegalArgumentException("Unknown owned OTLP signal");
    }

    static HttpSenderConfig bounded(HttpSenderConfig source) {
        return new HttpSenderConfig() {
            public java.net.URI getEndpoint() { return source.getEndpoint(); }
            public String getContentType() { return source.getContentType(); }
            public io.opentelemetry.sdk.common.export.Compressor getCompressor() { return source.getCompressor(); }
            public java.time.Duration getTimeout() { return source.getTimeout(); }
            public java.time.Duration getConnectTimeout() { return source.getConnectTimeout(); }
            public java.util.function.Supplier<java.util.Map<String, List<String>>> getHeadersSupplier() { return source.getHeadersSupplier(); }
            public io.opentelemetry.sdk.common.export.ProxyOptions getProxyOptions() { return source.getProxyOptions(); }
            public io.opentelemetry.sdk.common.export.RetryPolicy getRetryPolicy() { return source.getRetryPolicy(); }
            public javax.net.ssl.SSLContext getSslContext() { return source.getSslContext(); }
            public javax.net.ssl.X509TrustManager getTrustManager() { return source.getTrustManager(); }
            public java.util.concurrent.ExecutorService getExecutorService() { return source.getExecutorService(); }
            public long getMaxResponseBodySize() { return Math.min(source.getMaxResponseBodySize(), OtlpResponseAccounting.MAX_RESPONSE_BYTES); }
        };
    }

    private static final class SafeTransportFailure extends RuntimeException {
        SafeTransportFailure() { super("TeaQL OTLP transport failed; details masked", null, false, false); }
    }
}
