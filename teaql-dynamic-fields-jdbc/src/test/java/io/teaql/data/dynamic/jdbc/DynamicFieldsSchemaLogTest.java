package io.teaql.data.dynamic.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.teaql.dataservice.sql.SqlExecutionAdapter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.Test;

public class DynamicFieldsSchemaLogTest {

    @Test
    public void driverFailureDoesNotWriteExceptionTextEvenAtFineLevel() {
        Logger logger = Logger.getLogger(DynamicFieldsSchema.class.getName());
        Level originalLevel = logger.getLevel();
        List<LogRecord> records = new ArrayList<>();
        Handler capture = new Handler() {
            @Override public void publish(LogRecord record) { records.add(record); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        capture.setLevel(Level.ALL);
        logger.addHandler(capture);
        logger.setLevel(Level.FINE);
        try {
            SqlExecutionAdapter executor = (SqlExecutionAdapter) Proxy.newProxyInstance(
                    SqlExecutionAdapter.class.getClassLoader(),
                    new Class<?>[] { SqlExecutionAdapter.class },
                    (proxy, method, args) -> {
                        if (method.getName().equals("execute")) {
                            throw new IllegalStateException("DRIVER-CANARY PASSWORD-CANARY");
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
            DynamicFieldsSchema.ensureSchema(executor);
        } finally {
            logger.setLevel(originalLevel);
            logger.removeHandler(capture);
        }

        assertEquals(3, records.stream().filter(record -> record.getLevel() == Level.FINE).count());
        for (LogRecord record : records) {
            String rendered = java.text.MessageFormat.format(record.getMessage(), record.getParameters());
            if (record.getLevel() == Level.FINE) {
                assertTrue(rendered.contains("IllegalStateException"));
            }
            assertFalse(rendered.contains("DRIVER-CANARY"));
            assertFalse(rendered.contains("PASSWORD-CANARY"));
        }
    }
}
