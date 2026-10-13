package io.teaql.runtime;

import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class TelemetryStartupTest {
    @Test public void disabledShowsAllVariablesWithoutEchoingUnusedValues() {
        var config = TelemetryStartup.read(Map.of(TelemetryStartup.SERVICE_NAME, "PRIVATE-SERVICE"));
        assertFalse(config.enabled());
        String message = config.message();
        assertTrue(message.contains(TelemetryStartup.ENABLED));
        assertTrue(message.contains(TelemetryStartup.SERVICE_NAME));
        assertTrue(message.contains(TelemetryStartup.ENDPOINT));
        assertFalse(message.contains("PRIVATE-SERVICE"));
    }
    @Test public void credentialsAndTokenURLsAreRejectedWithoutEchoingThem() {
        for (String url : new String[]{"http://SECRET:password@localhost:4318", "http://localhost:4318?token=SECRET", "file:///SECRET"}) {
            var config = TelemetryStartup.read(Map.of(TelemetryStartup.ENABLED, "true",
                    TelemetryStartup.SERVICE_NAME, "orders", TelemetryStartup.ENDPOINT, url));
            assertFalse(config.enabled()); assertEquals("invalid-endpoint", config.error());
            assertFalse(config.message().contains("SECRET"));
        }
    }
    @Test public void validConfigurationIsFullRecordingAndDisabledDoesNotLoadFactory() {
        var config = TelemetryStartup.read(Map.of(TelemetryStartup.ENABLED, "true", TelemetryStartup.SERVICE_NAME,
                "orders", TelemetryStartup.ENDPOINT, "http://localhost:4318"));
        assertTrue(config.enabled()); assertTrue(config.message().contains("full recording"));
        ClassLoader forbidden = new ClassLoader(null) {
            @Override public java.util.Enumeration<java.net.URL> getResources(String name) {
                throw new AssertionError("Disabled mode loaded an optional factory");
            }
        };
        assertSame(RuntimeTelemetry.NOOP, TelemetryStartup.initialize(Map.of(), forbidden));
    }
}
