package io.teaql.businessid.jdbc;

import io.teaql.core.sql.portable.TeaQLDatabase;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class JdbcBusinessIdAllocatorTest {

    @Test
    public void concurrentWrongShapeTableReportsItsMissingColumns() {
        AtomicInteger inspections = new AtomicInteger();
        TeaQLDatabase database = (TeaQLDatabase) Proxy.newProxyInstance(
                TeaQLDatabase.class.getClassLoader(),
                new Class<?>[] {TeaQLDatabase.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getTableColumns")) {
                        return inspections.incrementAndGet() == 1
                                ? List.of()
                                : List.of(Map.of("column_name", "scope_key"));
                    }
                    if (method.getName().equals("execute")) {
                        throw new IllegalStateException("table already exists");
                    }
                    throw new AssertionError("Unexpected database call " + method.getName());
                });

        IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class,
                () -> new JdbcBusinessIdAllocator(database).ensureSchema(null));
        Assert.assertTrue(failure.getMessage(),
                failure.getMessage().contains("missing required columns"));
        Assert.assertEquals(2, inspections.get());
    }
}
