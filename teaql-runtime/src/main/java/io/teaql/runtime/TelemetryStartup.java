package io.teaql.runtime;

import java.net.URI;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TelemetryStartup {
    public static final String ENABLED = "TEAQL_TELEMETRY_ENABLED";
    public static final String SERVICE_NAME = "TEAQL_TELEMETRY_SERVICE_NAME";
    public static final String ENDPOINT = "TEAQL_TELEMETRY_OTLP_ENDPOINT";
    private static final String HINT = ENABLED + "=true " + SERVICE_NAME + "=<application-name> "
            + ENDPOINT + "=<collector-url>";
    private static final Set<String> ANNOUNCED = ConcurrentHashMap.newKeySet();

    private TelemetryStartup() {}

    public record Configuration(boolean enabled, String serviceName, URI endpoint, String error) {
        public String message() {
            if (error != null) return "TeaQL telemetry: disabled; configuration=" + error + ". " + HINT;
            if (!enabled) return "TeaQL telemetry: supported, disabled. Enable full masked diagnostics with " + HINT;
            return "TeaQL telemetry: starting standard module; full recording, masked SQL; "
                    + ENABLED + "=true " + SERVICE_NAME + "=" + serviceName + " " + ENDPOINT + "=" + endpoint;
        }
    }

    public static Configuration read(Map<String, String> environment) {
        String flag = environment.getOrDefault(ENABLED, "").trim();
        if (flag.isEmpty() || flag.equalsIgnoreCase("false")) return new Configuration(false, null, null, null);
        if (!flag.equalsIgnoreCase("true")) return new Configuration(false, null, null, "invalid-enable");
        String service = environment.getOrDefault(SERVICE_NAME, "").trim();
        if (!service.matches("[a-zA-Z0-9_.-]{1,120}")) return new Configuration(false, null, null, "invalid-service");
        try {
            URI endpoint = URI.create(environment.getOrDefault(ENDPOINT, ""));
            if (!("http".equals(endpoint.getScheme()) || "https".equals(endpoint.getScheme()))
                    || endpoint.getHost() == null || endpoint.getUserInfo() != null
                    || endpoint.getQuery() != null || endpoint.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            return new Configuration(true, service, endpoint, null);
        } catch (RuntimeException ignored) { return new Configuration(false, null, null, "invalid-endpoint"); }
    }

    public static void notice(String message) {
        try { System.getLogger("io.teaql.runtime.telemetry").log(System.Logger.Level.INFO, message); }
        catch (Throwable ignored) { /* diagnostic output must not change startup */ }
    }

    static RuntimeTelemetry initialize(Map<String, String> environment, ClassLoader loader) {
        Configuration config = read(environment);
        if (ANNOUNCED.add(config.message())) notice(config.message());
        if (!config.enabled()) return RuntimeTelemetry.NOOP;
        try {
            StandardTelemetryFactory factory = ServiceLoader.load(StandardTelemetryFactory.class, loader)
                    .findFirst().orElse(null);
            if (factory == null) {
                notice("TeaQL telemetry: standard module unavailable; install teaql-opentelemetry. " + HINT);
                return RuntimeTelemetry.NOOP;
            }
            RuntimeTelemetry telemetry = factory.create(config.serviceName(), config.endpoint());
            if (telemetry == null) throw new IllegalStateException();
            notice("TeaQL telemetry: enabled; full recording, traces, metrics, logs and masked SQL.");
            return telemetry;
        } catch (Throwable ignored) {
            notice("TeaQL telemetry: standard module initialization failed; business continues without export. " + HINT);
            return RuntimeTelemetry.NOOP;
        }
    }
}
