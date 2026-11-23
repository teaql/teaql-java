package io.teaql.runtime.log;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.TraceNode;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.RuntimeLogger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class StructuredLogManagerTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void optionalModuleUsesTheSameJsonLinesDestinationAndSafeCustomProjection() throws Exception {
        var logger = new RuntimeLogger(new RuntimeLogger.Options("application", List.of(), Map.of(), temporary.getRoot().toPath()));
        List<String> captured = new ArrayList<>();
        var context = new DefaultUserContext(null) {
            @Override public <T> T capability(Class<T> type) {
                if (type == RuntimeLogger.class) return type.cast(logger);
                if (type == CustomLogSink.class) return type.cast((CustomLogSink) captured::add);
                return null;
            }
        };
        var metadata = new ExecutionMetadata();
        metadata.setParameterizedQuery("SELECT id FROM customer WHERE password = ?");
        metadata.setParameters(List.of("private-password-value"));
        metadata.setDebugQuery("private-password-value"); metadata.setExecutionOutcome("retrying");
        LogManager.getInstance().writeExecutionLog(context, metadata);
        LogManager.getInstance().writeAuditLog(context, List.of(new TraceNode("changed private-password-value")),
                new AuditEvent("Customer", 42L, "UPDATE", List.of(new FieldChange("password", "private-password-value", "new-password-value"))));
        String output = Files.readString(logger.path());
        assertFalse(output.contains("private-password-value")); assertFalse(output.contains("new-password-value"));
        assertFalse(String.join("", captured).contains("private-password-value"));
        var records = output.lines().toList();
        assertEquals(3, records.size());
        for (var record : records) System.out.println("TEAQL_LOG_MANAGER_RECORD " + record);
        assertEquals(2, captured.size());
    }
}
