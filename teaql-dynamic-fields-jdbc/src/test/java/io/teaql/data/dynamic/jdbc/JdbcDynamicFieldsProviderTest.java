package io.teaql.data.dynamic.jdbc;

import io.teaql.data.dynamic.*;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class JdbcDynamicFieldsProviderTest {

    private JdbcDynamicFieldsProvider provider;

    @Before
    public void setUp() {
        // H2 in-memory database
        DataSource ds = new SimpleDataSource("jdbc:h2:mem:testdb_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        provider = new JdbcDynamicFieldsProvider(ds);
        provider.ensureSchema();
    }

    private final java.util.concurrent.atomic.AtomicLong idGen = new java.util.concurrent.atomic.AtomicLong(100000);

    private DynamicFieldContext globalCtx() {
        return new DynamicFieldContext() {
            @Override public String scopeType() { return "GLOBAL"; }
            @Override public String scopeId() { return "default"; }
            @Override public String userId() { return "test-user"; }
            @Override public String purpose() { return "unit test"; }
            @Override public String comment() { return "testing"; }
            @Override public boolean strictIntent() { return false; }
            @Override public long nextId(String typeName) { return idGen.incrementAndGet(); }
        };
    }

    private DynamicFieldDef createStringField(String code, String name) {
        DynamicFieldDef def = new DynamicFieldDef();
        def.setScope(DynamicFieldScope.global());
        def.setOwnerType("Platform");
        def.setCode(code);
        def.setName(name);
        def.setDataType(DynamicDataType.STRING);
        def.setStatus(DynamicFieldStatus.ACTIVE);
        return def;
    }

    private DynamicFieldDef createNumberField(String code, String name) {
        DynamicFieldDef def = new DynamicFieldDef();
        def.setScope(DynamicFieldScope.global());
        def.setOwnerType("Platform");
        def.setCode(code);
        def.setName(name);
        def.setDataType(DynamicDataType.NUMBER);
        def.setStatus(DynamicFieldStatus.ACTIVE);
        return def;
    }

    @Test
    public void testRegisterAndLoadFieldDef() {
        DynamicFieldContext context = globalCtx();
        DynamicFieldDef def = createStringField("customer_asset_no", "Customer Asset No");
        provider.registerFieldDef(context, def);

        DynamicFieldDef loaded = provider.loadFieldDef(context,
                DynamicFieldRef.of(DynamicFieldScope.global(), "Platform", "customer_asset_no"));
        assertNotNull(loaded);
        assertEquals("customer_asset_no", loaded.getCode());
        assertEquals(DynamicDataType.STRING, loaded.getDataType());
        assertTrue(loaded.isActive());
    }

    @Test
    public void testListFieldDefs() {
        DynamicFieldContext context = globalCtx();
        provider.registerFieldDef(context, createStringField("field_a", "Field A"));
        provider.registerFieldDef(context, createStringField("field_b", "Field B"));
        provider.registerFieldDef(context, createNumberField("field_c", "Field C"));

        List<DynamicFieldDef> defs = provider.listFieldDefs(context, "Platform");
        assertEquals(3, defs.size());
    }

    @Test
    public void testWriteAndReadStringValue() {
        DynamicFieldContext context = globalCtx();
        provider.registerFieldDef(context, createStringField("customer_asset_no", "Customer Asset No"));

        DynamicOwnerRef owner = DynamicOwnerRef.of("Platform", 1001L);
        provider.saveValue(context, DynamicSetCommand.of(
                owner, "customer_asset_no", DynamicDataType.STRING, "A-10086", "test", "set"));

        DynamicFieldValues values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectString("customer_asset_no"));
        assertTrue(values.isSelected("customer_asset_no"));
        assertEquals("A-10086", values.getString("customer_asset_no"));
    }

    @Test
    public void testWriteAndReadNumberValue() {
        DynamicFieldContext context = globalCtx();
        provider.registerFieldDef(context, createNumberField("priority_score", "Priority Score"));

        DynamicOwnerRef owner = DynamicOwnerRef.of("Platform", 1001L);
        provider.saveValue(context, DynamicSetCommand.of(
                owner, "priority_score", DynamicDataType.NUMBER, 80L, "test", "set"));

        DynamicFieldValues values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectNumber("priority_score"));
        assertEquals(80L, values.getNumber("priority_score").longValue());
    }

    @Test
    public void testUpsertOverwritesExistingValue() {
        DynamicFieldContext context = globalCtx();
        provider.registerFieldDef(context, createStringField("customer_asset_no", "Customer Asset No"));

        DynamicOwnerRef owner = DynamicOwnerRef.of("Platform", 1001L);

        // First write
        provider.saveValue(context, DynamicSetCommand.of(
                owner, "customer_asset_no", DynamicDataType.STRING, "OLD", "test", "set"));

        // Overwrite
        provider.saveValue(context, DynamicSetCommand.of(
                owner, "customer_asset_no", DynamicDataType.STRING, "NEW", "test", "update"));

        DynamicFieldValues values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectString("customer_asset_no"));
        assertEquals("NEW", values.getString("customer_asset_no"));
    }

    @Test
    public void testBatchLoadValues() {
        DynamicFieldContext context = globalCtx();
        provider.registerFieldDef(context, createStringField("customer_asset_no", "Customer Asset No"));

        DynamicOwnerRef owner1 = DynamicOwnerRef.of("Platform", 1001L);
        DynamicOwnerRef owner2 = DynamicOwnerRef.of("Platform", 1002L);

        provider.saveValue(context, DynamicSetCommand.of(
                owner1, "customer_asset_no", DynamicDataType.STRING, "A-001", "test", "set"));
        provider.saveValue(context, DynamicSetCommand.of(
                owner2, "customer_asset_no", DynamicDataType.STRING, "A-002", "test", "set"));

        Map<DynamicOwnerRef, DynamicFieldValues> result = provider.loadValues(
                context, List.of(owner1, owner2),
                new DynamicFieldSelection().selectString("customer_asset_no"));

        assertEquals("A-001", result.get(owner1).getString("customer_asset_no"));
        assertEquals("A-002", result.get(owner2).getString("customer_asset_no"));
    }

    @Test
    public void testDeleteValue() {
        DynamicFieldContext context = globalCtx();
        DynamicFieldDef def = createStringField("customer_asset_no", "Customer Asset No");
        provider.registerFieldDef(context, def);

        DynamicOwnerRef owner = DynamicOwnerRef.of("Platform", 1001L);
        provider.saveValue(context, DynamicSetCommand.of(
                owner, "customer_asset_no", DynamicDataType.STRING, "A-10086", "test", "set"));

        // Verify written
        DynamicFieldValues values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectString("customer_asset_no"));
        assertEquals("A-10086", values.getString("customer_asset_no"));

        // Reload def to get its ID
        DynamicFieldDef loaded = provider.loadFieldDef(context,
                DynamicFieldRef.of(DynamicFieldScope.global(), "Platform", "customer_asset_no"));

        // Delete
        provider.deleteValue(context, DynamicValueRef.of(owner, loaded.getId()));

        // Verify deleted - should return empty (no rows match)
        values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectString("customer_asset_no"));
        assertEquals(0, values.size());
    }

    @Test
    public void testFacadeEndToEnd() {
        DynamicFieldContext context = globalCtx();
        provider.registerFieldDef(context, createStringField("customer_asset_no", "Customer Asset No"));
        provider.registerFieldDef(context, createNumberField("priority_score", "Priority Score"));

        DefaultDynamicFieldsFacade facade = new DefaultDynamicFieldsFacade(provider);

        // Write via facade
        facade.purpose("test").comment("set asset")
                .owner("Platform", 1001L)
                .string("customer_asset_no")
                .set("A-10086");

        facade.purpose("test").comment("set priority")
                .owner("Platform", 1001L)
                .number("priority_score")
                .set(80L);

        // Read via facade
        String assetNo = facade.owner("Platform", 1001L)
                .string("customer_asset_no")
                .get();
        assertEquals("A-10086", assetNo);

        Number priority = facade.owner("Platform", 1001L)
                .number("priority_score")
                .get();
        assertEquals(80L, priority.longValue());
    }

    // ─── Simple DataSource for testing ──────────────────────────────────

    private static class SimpleDataSource implements DataSource {
        private final String url;

        SimpleDataSource(String url) {
            this.url = url;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() { return Logger.getLogger("SimpleDataSource"); }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
