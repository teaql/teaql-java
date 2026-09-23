package io.teaql.examples.businessid;

import io.teaql.businessid.jdbc.JdbcBusinessIdAllocator;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityKey;
import io.teaql.core.UserContext;
import io.teaql.core.SchemaExecutor;
import io.teaql.core.businessid.*;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.runtime.businessid.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Assert;
import org.junit.Test;

public class BusinessIdRuntimeExampleTest {
    private static final BusinessIdDefinition ORDER_NUMBER =
            BusinessIdDefinition.dailySequence("order_number", "CO", "commerce_order");

    public record OrderNumber(String value) {
        public OrderNumber {
            if (value == null || !value.matches("CO-\\d{8}-\\d{8}")) {
                throw new IllegalArgumentException("invalid OrderNumber: " + value);
            }
        }
    }

    private static final class Order extends BaseEntity implements BusinessIdSlot {
        private OrderNumber orderNumber;

        private Order(long id) {
            updateId(id);
        }

        @Override
        public String typeName() {
            return "CommerceOrder";
        }

        @Override
        public String currentValue() {
            return orderNumber == null ? null : orderNumber.value();
        }

        @Override
        public boolean newAggregate() {
            return newItem();
        }

        @Override
        public void assignCanonicalValue(String value) {
            OrderNumber next = new OrderNumber(value);
            OrderNumber previous = orderNumber;
            orderNumber = next;
            handleUpdate("order_number", previous, next);
        }

        private OrderNumber orderNumber() {
            return orderNumber;
        }

        private void markPersisted() {
            set$status(io.teaql.core.EntityStatus.PERSISTED);
        }
    }

    @Test
    public void inMemoryProfileIsScopedTypedAndRetryStable() {
        UserContext context = context(LocalDate.of(2026, 9, 20));
        DefaultBusinessIdService service =
                new DefaultBusinessIdService(new InMemoryBusinessIdAllocator());
        context.putAttribute(BusinessIdService.class.getName(), service);

        Order first = new Order(101);
        BusinessIdValue initial = context.businessIds().ensure(
                context, ORDER_NUMBER, "tenant-a", "commerce_order", first);
        Assert.assertEquals("CO-20260920-00000001", initial.value());
        Assert.assertEquals(new OrderNumber(initial.value()), first.orderNumber());
        Assert.assertEquals(
                first.orderNumber(),
                first.getEntityMutationLedger().get(
                        new EntityKey("CommerceOrder", 101L), "order_number"));

        // Simulated provider failure: the same pending entity/ledger is retried.
        BusinessIdValue retry = context.businessIds().ensure(
                context, ORDER_NUMBER, "tenant-a", "commerce_order", first);
        Assert.assertEquals(initial, retry);

        Order second = new Order(102);
        Assert.assertEquals(
                "CO-20260920-00000002",
                context.businessIds().ensure(
                        context, ORDER_NUMBER, "tenant-a", "commerce_order", second).value());

        Order otherTenant = new Order(103);
        Assert.assertEquals(
                "CO-20260920-00000001",
                context.businessIds().ensure(
                        context, ORDER_NUMBER, "tenant-b", "commerce_order", otherTenant).value());

        UserContext nextDay = context(LocalDate.of(2026, 9, 21));
        nextDay.putAttribute(BusinessIdService.class.getName(), service);
        Assert.assertEquals(
                "CO-20260921-00000001",
                nextDay.businessIds().ensure(
                        nextDay, ORDER_NUMBER, "tenant-a", "commerce_order", new Order(104)).value());

        Order establishedWithoutNumber = new Order(105);
        establishedWithoutNumber.markPersisted();
        BusinessIdException immutable = Assert.assertThrows(
                BusinessIdException.class,
                () -> context.businessIds().ensure(
                        context,
                        ORDER_NUMBER,
                        "tenant-a",
                        "commerce_order",
                        establishedWithoutNumber));
        Assert.assertEquals(BusinessIdErrorCode.BUSINESS_ID_IMMUTABLE, immutable.getCode());
    }

    @Test
    public void sqliteAllocatorIsExplicitCrossInstanceAndRestartSafe() throws Exception {
        Path databasePath = Files.createTempFile("teaql-business-id", ".db");
        try {
            BusinessIdPlan plan = new DailySequenceBusinessIdProfile().plan(
                    new BusinessIdGenerationRequest(
                            ORDER_NUMBER,
                            "tenant-a",
                            "commerce_order",
                            LocalDate.of(2026, 9, 20)));

            try (Connection schemaConnection = open(databasePath)) {
                JdbcBusinessIdAllocator allocator =
                        new JdbcBusinessIdAllocator(database(schemaConnection));
                UserContext context = context(LocalDate.of(2026, 9, 20), allocator);
                context.putAttribute(
                        SchemaExecutor.class.getName(),
                        noOpSchemaExecutor());
                context.ensureSchema();
                Assert.assertNotNull(context.businessIds());
            }

            CountDownLatch start = new CountDownLatch(1);
            ExecutorService workers = Executors.newFixedThreadPool(2);
            try {
                List<Future<List<Long>>> futures = new ArrayList<>();
                for (int worker = 0; worker < 2; worker++) {
                    futures.add(workers.submit(() -> {
                        try (Connection connection = open(databasePath)) {
                            JdbcBusinessIdAllocator allocator =
                                    new JdbcBusinessIdAllocator(database(connection));
                            start.await();
                            List<Long> allocated = new ArrayList<>();
                            for (int index = 0; index < 25; index++) {
                                allocated.add(allocator.allocate(plan).sequence());
                            }
                            return allocated;
                        }
                    }));
                }
                start.countDown();
                Set<Long> allocated = new HashSet<>();
                for (Future<List<Long>> future : futures) {
                    allocated.addAll(future.get(15, TimeUnit.SECONDS));
                }
                Assert.assertEquals(50, allocated.size());
            } finally {
                workers.shutdownNow();
            }

            try (Connection restarted = open(databasePath)) {
                JdbcBusinessIdAllocator allocator =
                        new JdbcBusinessIdAllocator(database(restarted));
                Assert.assertEquals(51, allocator.allocate(plan).sequence());
            }
        } finally {
            Files.deleteIfExists(databasePath);
        }
    }

    @Test
    public void sqliteAllocatorReportsMissingSchemaWithoutRetryExhaustion() throws Exception {
        Path databasePath = Files.createTempFile("teaql-business-id-missing-schema", ".db");
        try (Connection connection = open(databasePath)) {
            JdbcBusinessIdAllocator allocator =
                    new JdbcBusinessIdAllocator(database(connection));
            BusinessIdPlan plan = new DailySequenceBusinessIdProfile().plan(
                    new BusinessIdGenerationRequest(
                            ORDER_NUMBER, "tenant-a", "commerce_order",
                            LocalDate.of(2026, 9, 20)));

            IllegalStateException failure = Assert.assertThrows(
                    IllegalStateException.class, () -> allocator.allocate(plan));
            Assert.assertTrue(failure.getMessage(),
                    failure.getMessage().contains("context.ensureSchema()"));
            Assert.assertNotNull(failure.getCause());
        } finally {
            Files.deleteIfExists(databasePath);
        }
    }

    @Test
    public void sqliteAllocatorRejectsMalformedSequenceRowWithoutRetryExhaustion()
            throws Exception {
        Path databasePath = Files.createTempFile("teaql-business-id-malformed", ".db");
        try (Connection connection = open(databasePath)) {
            JdbcBusinessIdAllocator allocator =
                    new JdbcBusinessIdAllocator(database(connection));
            LocalDate businessDate = LocalDate.of(2026, 9, 20);
            BusinessIdPlan plan = new DailySequenceBusinessIdProfile().plan(
                    new BusinessIdGenerationRequest(
                            ORDER_NUMBER, "tenant-a", "commerce_order", businessDate));
            UserContext context = context(businessDate, allocator);
            context.putAttribute(SchemaExecutor.class.getName(), noOpSchemaExecutor());
            context.ensureSchema();
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO teaql_business_id_space"
                            + " (scope_key, current_value, version, updated_at)"
                            + " VALUES (?, 'invalid', 1, 0)")) {
                insert.setString(1, plan.scope().canonicalKey());
                Assert.assertEquals(1, insert.executeUpdate());
            }

            IllegalStateException failure = Assert.assertThrows(
                    IllegalStateException.class, () -> allocator.allocate(plan));
            Assert.assertTrue(failure.getMessage(),
                    failure.getMessage().contains("current_value"));
        } finally {
            Files.deleteIfExists(databasePath);
        }
    }

    @Test
    public void sqliteAllocatorRejectsIncompleteExistingSchema() throws Exception {
        Path databasePath = Files.createTempFile("teaql-business-id-incomplete-schema", ".db");
        try (Connection connection = open(databasePath)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE teaql_business_id_space (scope_key TEXT PRIMARY KEY)");
            }
            JdbcBusinessIdAllocator allocator =
                    new JdbcBusinessIdAllocator(database(connection));
            UserContext context = context(LocalDate.of(2026, 9, 20), allocator);
            context.putAttribute(SchemaExecutor.class.getName(), noOpSchemaExecutor());

            IllegalStateException failure = Assert.assertThrows(
                    IllegalStateException.class, context::ensureSchema);
            Assert.assertTrue(failure.getMessage(),
                    failure.getMessage().contains("missing required columns"));
        } finally {
            Files.deleteIfExists(databasePath);
        }
    }

    @Test
    public void sqliteAllocatorRejectsExhaustedAndOverflowedSequencesWithoutMutation()
            throws Exception {
        Path databasePath = Files.createTempFile("teaql-business-id-range", ".db");
        try {
            LocalDate businessDate = LocalDate.of(2026, 9, 20);
            BusinessIdPlan plan = new DailySequenceBusinessIdProfile().plan(
                    new BusinessIdGenerationRequest(
                            ORDER_NUMBER, "tenant-a", "commerce_order", businessDate));
            try (Connection connection = open(databasePath)) {
                JdbcBusinessIdAllocator allocator =
                        new JdbcBusinessIdAllocator(database(connection));
                UserContext context = context(businessDate, allocator);
                context.putAttribute(SchemaExecutor.class.getName(), noOpSchemaExecutor());
                context.ensureSchema();

                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO teaql_business_id_space"
                                + " (scope_key, current_value, version, updated_at)"
                                + " VALUES (?, ?, 7, 0)")) {
                    insert.setString(1, plan.scope().canonicalKey());
                    insert.setLong(2, plan.maximumSequence());
                    insert.executeUpdate();
                }

                for (long storedValue : new long[] {plan.maximumSequence(), Long.MAX_VALUE}) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE teaql_business_id_space SET current_value = ?, version = 7"
                                    + " WHERE scope_key = ?")) {
                        update.setLong(1, storedValue);
                        update.setString(2, plan.scope().canonicalKey());
                        Assert.assertEquals(1, update.executeUpdate());
                    }

                    BusinessIdException failure = Assert.assertThrows(
                            BusinessIdException.class, () -> allocator.allocate(plan));
                    Assert.assertEquals(
                            BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED,
                            failure.getCode());
                    try (PreparedStatement read = connection.prepareStatement(
                            "SELECT current_value, version FROM teaql_business_id_space"
                                    + " WHERE scope_key = ?")) {
                        read.setString(1, plan.scope().canonicalKey());
                        try (ResultSet row = read.executeQuery()) {
                            Assert.assertTrue(row.next());
                            Assert.assertEquals(storedValue, row.getLong(1));
                            Assert.assertEquals(7L, row.getLong(2));
                        }
                    }
                }
            }
        } finally {
            Files.deleteIfExists(databasePath);
        }
    }

    private static UserContext context(LocalDate date) {
        return context(date, null);
    }

    static UserContext context(
            LocalDate date, JdbcBusinessIdAllocator businessIdInfrastructure) {
        TeaQLRuntime.Builder builder = TeaQLRuntime.builder()
                .metadata(new EntityMetaFactory() {
                    @Override
                    public EntityDescriptor resolveEntityDescriptor(String type) {
                        return null;
                    }

                    @Override
                    public void register(EntityDescriptor type) {}

                    @Override
                    public List<EntityDescriptor> allEntityDescriptors() {
                        return List.of();
                    }
                })
                .executionLogging(false);
        if (businessIdInfrastructure != null) {
            builder.businessIdInfrastructure(businessIdInfrastructure);
        }
        TeaQLRuntime runtime = builder.build();
        DefaultUserContext context = new DefaultUserContext(runtime);
        context.putAttribute(BusinessClock.class.getName(),
                (BusinessClock) ignored -> date);
        context.putAttribute(BusinessIdProfileFactory.class.getName(),
                new DefaultBusinessIdProfileFactory());
        return context;
    }

    private static Connection open(Path path) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout=5000");
        }
        return connection;
    }

    static SchemaExecutor noOpSchemaExecutor() {
        return (SchemaExecutor) java.lang.reflect.Proxy.newProxyInstance(
                SchemaExecutor.class.getClassLoader(),
                new Class<?>[] {SchemaExecutor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("ensureSchema")) return null;
                    Class<?> resultType = method.getReturnType();
                    if (resultType == boolean.class) return false;
                    if (resultType == int.class) return 0;
                    if (resultType == long.class) return 0L;
                    return null;
                });
    }

    static TeaQLDatabase database(Connection connection) {
        return new TeaQLDatabase() {
            @Override
            public List<Map<String, Object>> query(String sql, Object[] args) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    bind(statement, args);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        List<Map<String, Object>> rows = new ArrayList<>();
                        ResultSetMetaData metadata = resultSet.getMetaData();
                        while (resultSet.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int column = 1; column <= metadata.getColumnCount(); column++) {
                                row.put(metadata.getColumnLabel(column).toLowerCase(Locale.ROOT),
                                        resultSet.getObject(column));
                            }
                            rows.add(row);
                        }
                        return rows;
                    }
                } catch (SQLException error) {
                    throw new RuntimeException(error);
                }
            }

            @Override
            public int executeUpdate(String sql, Object[] args) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    bind(statement, args);
                    return statement.executeUpdate();
                } catch (SQLException error) {
                    throw new RuntimeException(error);
                }
            }

            @Override
            public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void execute(String sql) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                } catch (SQLException error) {
                    throw new RuntimeException(error);
                }
            }

            @Override
            public void executeInTransaction(Runnable action) {
                try {
                    boolean original = connection.getAutoCommit();
                    connection.setAutoCommit(false);
                    try {
                        action.run();
                        connection.commit();
                    } catch (RuntimeException error) {
                        connection.rollback();
                        throw error;
                    } finally {
                        connection.setAutoCommit(original);
                    }
                } catch (SQLException error) {
                    throw new RuntimeException(error);
                }
            }

            @Override
            public List<Map<String, Object>> getTableColumns(String tableName) {
                try {
                    List<Map<String, Object>> columns = new ArrayList<>();
                    java.sql.DatabaseMetaData metadata = connection.getMetaData();
                    for (String name : List.of(tableName, tableName.toUpperCase(Locale.ROOT))) {
                        try (ResultSet result = metadata.getColumns(
                                connection.getCatalog(), connection.getSchema(), name, null)) {
                            while (result.next()) {
                                columns.add(Map.of("column_name", result.getString("COLUMN_NAME")));
                            }
                        }
                        if (!columns.isEmpty()) break;
                    }
                    return columns;
                } catch (SQLException error) {
                    throw new RuntimeException(error);
                }
            }

            private void bind(PreparedStatement statement, Object[] args) throws SQLException {
                if (args == null) return;
                for (int index = 0; index < args.length; index++) {
                    statement.setObject(index + 1, args[index]);
                }
            }
        };
    }
}
