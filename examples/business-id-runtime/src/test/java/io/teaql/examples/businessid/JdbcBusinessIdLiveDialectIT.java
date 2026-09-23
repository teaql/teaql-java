package io.teaql.examples.businessid;

import io.teaql.businessid.jdbc.JdbcBusinessIdAllocator;
import io.teaql.core.SchemaExecutor;
import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessIdDefinition;
import io.teaql.core.businessid.BusinessIdGenerationRequest;
import io.teaql.core.businessid.BusinessIdPlan;
import io.teaql.runtime.businessid.DailySequenceBusinessIdProfile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/** Live, independent-connection Business ID allocation across PostgreSQL and MySQL. */
public class JdbcBusinessIdLiveDialectIT {
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 9, 20);
    private static final int ALLOCATIONS_PER_INSTANCE = 40;

    @Test
    public void postgresAllocatesAcrossInstancesAndRestart() throws Exception {
        verify("POSTGRES");
    }

    @Test
    public void mysqlAllocatesAcrossInstancesAndRestart() throws Exception {
        verify("MYSQL");
    }

    private void verify(String dialect) throws Exception {
        String prefix = "TEAQL_TEST_" + dialect + "_";
        String url = System.getenv(prefix + "URL");
        String user = System.getenv(prefix + "USER");
        String password = System.getenv(prefix + "PASSWORD");
        if (url == null || user == null || password == null) {
            if (Boolean.parseBoolean(System.getenv("TEAQL_REQUIRE_LIVE_DB"))) {
                Assert.fail(dialect + " Business ID live gate requires URL, USER, and PASSWORD");
            }
            Assume.assumeTrue(dialect + " live database is not configured", false);
        }
        Assert.assertTrue("Use a dedicated teaql_live_* database for " + dialect,
                url.contains("teaql_live_"));

        BusinessIdDefinition definition = BusinessIdDefinition.dailySequence(
                "order_number", "CO", "commerce_order");
        BusinessIdPlan plan = new DailySequenceBusinessIdProfile().plan(
                new BusinessIdGenerationRequest(
                        definition, "tenant-" + UUID.randomUUID(),
                        "commerce_order", BUSINESS_DATE));

        try (Connection schemaConnection = DriverManager.getConnection(url, user, password)) {
            JdbcBusinessIdAllocator allocator = new JdbcBusinessIdAllocator(
                    BusinessIdRuntimeExampleTest.database(schemaConnection));
            UserContext context = BusinessIdRuntimeExampleTest.context(BUSINESS_DATE, allocator);
            context.putAttribute(SchemaExecutor.class.getName(),
                    BusinessIdRuntimeExampleTest.noOpSchemaExecutor());
            context.ensureSchema();
        }

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            List<Future<List<Long>>> futures = new ArrayList<>();
            for (int worker = 0; worker < 2; worker++) {
                futures.add(workers.submit(() -> {
                    try (Connection connection = DriverManager.getConnection(url, user, password)) {
                        JdbcBusinessIdAllocator allocator = new JdbcBusinessIdAllocator(
                                BusinessIdRuntimeExampleTest.database(connection));
                        Assert.assertTrue("Concurrent start timed out",
                                start.await(15, TimeUnit.SECONDS));
                        List<Long> sequences = new ArrayList<>();
                        for (int index = 0; index < ALLOCATIONS_PER_INSTANCE; index++) {
                            sequences.add(allocator.allocate(plan).sequence());
                        }
                        return sequences;
                    }
                }));
            }
            start.countDown();
            Set<Long> allocated = new HashSet<>();
            for (Future<List<Long>> future : futures) {
                allocated.addAll(future.get(45, TimeUnit.SECONDS));
            }
            int total = 2 * ALLOCATIONS_PER_INSTANCE;
            Assert.assertEquals(total, allocated.size());
            for (long sequence = 1; sequence <= total; sequence++) {
                Assert.assertTrue("Missing sequence " + sequence, allocated.contains(sequence));
            }
        } finally {
            workers.shutdownNow();
        }

        try (Connection restarted = DriverManager.getConnection(url, user, password)) {
            JdbcBusinessIdAllocator allocator = new JdbcBusinessIdAllocator(
                    BusinessIdRuntimeExampleTest.database(restarted));
            Assert.assertEquals(2L * ALLOCATIONS_PER_INSTANCE + 1,
                    allocator.allocate(plan).sequence());
        }
    }
}
