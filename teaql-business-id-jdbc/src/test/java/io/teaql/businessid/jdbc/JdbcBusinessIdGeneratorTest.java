package io.teaql.businessid.jdbc;

import io.teaql.core.Entity;
import io.teaql.core.UserContext;
import io.teaql.core.SchemaExecutor;
import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.businessid.BusinessIdErrorCode;
import io.teaql.core.businessid.BusinessIdException;
import io.teaql.core.businessid.BusinessIdSchemaContributor;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.meta.SimplePropertyType;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class JdbcBusinessIdGeneratorTest {

    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 9, 20);

    private Connection connection;
    private TeaQLDatabase testDatabase;
    private JdbcBusinessIdGenerator generator;
    private EntityDescriptor entityDesc;
    private PropertyDescriptor propDesc;
    private UserContext dummyContext;
    private Entity dummyEntity;

    @Before
    public void setUp() throws Exception {
        // Use in-memory SQLite for testing
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        
        testDatabase = new TeaQLDatabase() {
            @Override
            public List<Map<String, Object>> query(String sql, Object[] args) {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            stmt.setObject(i + 1, args[i]);
                        }
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Map<String, Object>> result = new ArrayList<>();
                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= colCount; i++) {
                                row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                            }
                            result.add(row);
                        }
                        return result;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public int executeUpdate(String sql, Object[] args) {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            stmt.setObject(i + 1, args[i]);
                        }
                    }
                    return stmt.executeUpdate();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
                return new int[0];
            }

            @Override
            public void execute(String sql) {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.execute();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void executeInTransaction(Runnable action) {
                try {
                    boolean autoCommit = connection.getAutoCommit();
                    connection.setAutoCommit(false);
                    try {
                        action.run();
                        connection.commit();
                    } catch (Exception e) {
                        connection.rollback();
                        throw new RuntimeException(e);
                    } finally {
                        connection.setAutoCommit(autoCommit);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public List<Map<String, Object>> getTableColumns(String tableName) {
                return new ArrayList<>();
            }
        };

        generator = new JdbcBusinessIdGenerator(testDatabase);

        entityDesc = new EntityDescriptor();
        entityDesc.setType("Order");
        
        propDesc = new PropertyDescriptor("orderNumber", new SimplePropertyType(String.class));
        
        DefaultUserContext context = new DefaultUserContext(
                TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory()).build());
        context.putAttribute(BusinessClock.class.getName(),
                (BusinessClock) ignored -> FIXED_DATE);
        context.putAttribute(BusinessIdSchemaContributor.class.getName(), generator);
        context.putAttribute(SchemaExecutor.class.getName(),
                (SchemaExecutor) java.lang.reflect.Proxy.newProxyInstance(
                        SchemaExecutor.class.getClassLoader(),
                        new Class<?>[] {SchemaExecutor.class},
                        (proxy, method, args) -> null));
        dummyContext = context;
        dummyEntity = null;
    }
    
    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void testGenerateBusinessId_Success() {
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 6");
        dummyContext.ensureSchema();

        String id1 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);
        String id2 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);

        String dateStr = "20260920";
        
        Assert.assertEquals("ORD" + dateStr + "000001", id1);
        Assert.assertEquals("ORD" + dateStr + "000002", id2);
    }
    
    @Test
    public void testGenerateBusinessId_MultipleKeys() {
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 4");
        dummyContext.ensureSchema();
        String id1 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);
        
        PropertyDescriptor anotherProp = new PropertyDescriptor("logisticsNumber", new SimplePropertyType(String.class));
        anotherProp.getAdditionalInfo().put("business_id_rule", "LOG, 4");
        String id2 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, anotherProp);
        
        String dateStr = "20260920";
        
        Assert.assertEquals("ORD" + dateStr + "0001", id1);
        Assert.assertEquals("LOG" + dateStr + "0001", id2);
    }

    @Test
    public void testConstructorAndGenerationDoNotCreateSchema() throws Exception {
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 6");
        Assert.assertTrue(testTableNames().isEmpty());

        TeaQLRuntimeException missing = Assert.assertThrows(
                TeaQLRuntimeException.class,
                () -> generator.generateBusinessId(
                        dummyContext, dummyEntity, entityDesc, propDesc));
        Assert.assertTrue(missing.getMessage().contains("context.ensureSchema()"));
        Assert.assertTrue(testTableNames().isEmpty());

        dummyContext.ensureSchema();
        dummyContext.ensureSchema();
        Assert.assertEquals(List.of("teaql_biz_sequence"), testTableNames());
        Assert.assertEquals("ORD20260920000001",
                generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc));
    }

    @Test
    public void testFixedContextDateAndRangeExhaustion() throws Exception {
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 1");
        dummyContext.ensureSchema();
        for (int i = 1; i <= 9; i++) {
            Assert.assertEquals("ORD20260920" + i,
                    generator.generateBusinessId(
                            dummyContext, dummyEntity, entityDesc, propDesc));
        }
        BusinessIdException exhausted = Assert.assertThrows(BusinessIdException.class,
                () -> generator.generateBusinessId(
                        dummyContext, dummyEntity, entityDesc, propDesc));
        Assert.assertEquals(BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED,
                exhausted.getCode());
        ((DefaultUserContext) dummyContext).putAttribute(BusinessClock.class.getName(),
                (BusinessClock) ignored -> LocalDate.of(2026, 9, 21));
        Assert.assertEquals("ORD202609211",
                generator.generateBusinessId(
                        dummyContext, dummyEntity, entityDesc, propDesc));
    }

    @Test
    public void testConcurrentReadersCannotReuseOneLegacySequenceNumber() throws Exception {
        AtomicLong stored = new AtomicLong(1);
        AtomicInteger reads = new AtomicInteger();
        CyclicBarrier firstReads = new CyclicBarrier(2);
        TeaQLDatabase competingDatabase = new TeaQLDatabase() {
            @Override
            public List<Map<String, Object>> query(String sql, Object[] args) {
                long snapshot = stored.get();
                if (reads.incrementAndGet() <= 2) {
                    try {
                        firstReads.await(5, TimeUnit.SECONDS);
                    } catch (Exception failure) {
                        throw new RuntimeException(failure);
                    }
                }
                return List.of(Map.of("current_value", snapshot));
            }

            @Override
            public int executeUpdate(String sql, Object[] args) {
                long next = ((Number) args[0]).longValue();
                long expected = ((Number) args[2]).longValue();
                return stored.compareAndSet(expected, next) ? 1 : 0;
            }

            @Override public int[] batchUpdate(String sql, List<Object[]> args) {
                throw new UnsupportedOperationException();
            }

            @Override public void execute(String sql) {
                throw new AssertionError("allocation must not execute DDL");
            }

            @Override public void executeInTransaction(Runnable action) {
                action.run();
            }

            @Override public List<Map<String, Object>> getTableColumns(String table) {
                return List.of();
            }
        };
        JdbcBusinessIdGenerator competing = new JdbcBusinessIdGenerator(competingDatabase);
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 6");
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = workers.submit(() -> competing.generateBusinessId(
                    dummyContext, dummyEntity, entityDesc, propDesc));
            Future<String> second = workers.submit(() -> competing.generateBusinessId(
                    dummyContext, dummyEntity, entityDesc, propDesc));
            Assert.assertEquals(Set.of("ORD20260920000002", "ORD20260920000003"),
                    Set.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)));
            Assert.assertEquals(3L, stored.get());
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    public void testRejectsUnsafeLegacyTableName() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> new JdbcBusinessIdGenerator(
                        testDatabase, "teaql_biz_sequence;DROP TABLE x"));
    }

    @Test
    public void testZeroRowInsertWithoutObservedSequenceFailsImmediately() {
        AtomicInteger queries = new AtomicInteger();
        TeaQLDatabase noInsert = (TeaQLDatabase) java.lang.reflect.Proxy.newProxyInstance(
                TeaQLDatabase.class.getClassLoader(),
                new Class<?>[] {TeaQLDatabase.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("query")) {
                        queries.incrementAndGet();
                        return List.of();
                    }
                    if (method.getName().equals("executeUpdate")) return 0;
                    throw new AssertionError("Unexpected database call " + method.getName());
                });
        JdbcBusinessIdGenerator noInsertGenerator = new JdbcBusinessIdGenerator(noInsert);
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 6");

        TeaQLRuntimeException failure = Assert.assertThrows(
                TeaQLRuntimeException.class,
                () -> noInsertGenerator.generateBusinessId(
                        dummyContext, dummyEntity, entityDesc, propDesc));
        Assert.assertTrue(failure.getMessage().contains("without a matching row"));
        Assert.assertEquals(2, queries.get());
    }

    private List<String> testTableNames() throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type = 'table'"
                        + " AND name = 'teaql_biz_sequence'");
             ResultSet rows = statement.executeQuery()) {
            List<String> names = new ArrayList<>();
            while (rows.next()) names.add(rows.getString(1));
            return names;
        }
    }
}
