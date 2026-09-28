package io.teaql.core.sql.portable;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Test;

public class PortableLogPrivacyTest {
    @Test public void legacyBootstrapLoggerDoesNotBypassRuntimeLogSwitch() {
        var original = System.out;
        var bytes = new ByteArrayOutputStream();
        try (var output = new PrintStream(bytes)) {
            System.setOut(output);
            PortableSQLRepository.logInfo("INSERT INTO customer VALUES ('PRIVATE-SEED-CANARY')");
        } finally {
            System.setOut(original);
        }
        String text = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("Schema diagnostics belong to the governed runtime adapter", "", text);
    }
}
