package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.checkerfixadvanced.CheckerFixAdvancedUserContext;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.GeneratedRuntimeModule;
import com.teaql.checkerfixadvanced.Q;
import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Application-owned source; its generated dependency is never patched to make the test pass. */
public class GeneratedFixEExampleTest {
    static final class Context extends DefaultUserContext implements CheckerFixAdvancedUserContext {
        Context(TeaQLRuntime runtime) { super(runtime); }
    }
    record DatabaseBridge(SqlExecutionAdapter sql) implements TeaQLDatabase {
        public List<Map<String, Object>> query(String query, Object[] args) { return sql.queryForList(query, args); }
        public int executeUpdate(String query, Object[] args) { return sql.update(query, args); }
        public int[] batchUpdate(String query, List<Object[]> args) { return sql.batchUpdate(query, args); }
        public void execute(String query) { sql.execute(query); }
        public void executeInTransaction(Runnable action) { sql.executeInTransaction(action); }
        public List<Map<String, Object>> getTableColumns(String table) {
            throw new UnsupportedOperationException("Schema inspection belongs to the dialect");
        }
    }

    @Test public void localGeneratedSaveAndEConsumeTheBusinessClock() throws Exception {
        var first = LocalDateTime.of(2032, 2, 29, 10, 15, 30);
        var second = first.plusHours(25);
        var time = new AtomicReference<>(first);
        BusinessClock clock = new BusinessClock() {
            public LocalDate businessDate(UserContext context) { return time.get().toLocalDate(); }
            public LocalDateTime businessDateTime(UserContext context) { return time.get(); }
        };
        var source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:file:teaql-fix-e-" + UUID.randomUUID() + "?mode=memory&cache=shared");
        // Keep this named in-memory database alive across provider connections.
        try (var keeper = source.getConnection()) {
            var sql = new JdbcSqlExecutor(source);
            var ids = new IdSpaceIdGenerator(new DatabaseBridge(sql));
            ids.ensureIdSpaceTable();
            var service = new SqliteDataServiceExecutor("sqlite", sql, source);
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default", service).dataService("sqlite", service)
                    .idGenerationService(ids).businessClock(clock).build().install(GeneratedRuntimeModule.module());
            UserContext context = new Context(runtime);
            context.ensureSchema();
            var order = Q.purchaseOrders().comment("Prepare the synthetic clock fixture")
                    .purpose("Verify generated audited saves and expression diagnostics").newEntity(context);
            order.updateContactPhone("5550100");
            order.updateInternalNote(null);
            order.auditAs("Create the synthetic Fix order").save(context);
            assertEquals(first, E.purchaseOrder(order).getCreateTime().eval());
            assertEquals(first, E.purchaseOrder(order).getUpdateTime().eval());
            assertEquals(Long.valueOf(1), E.purchaseOrder(order).getTenant().getId().eval());
            assertEquals("fallback", E.purchaseOrder(order).getInternalNote().orIfNull("fallback"));

            var partial = Q.purchaseOrdersWithMinimalFields().withIdIs(order.getId()).limit(1)
                    .comment("Load the identity projection without eager data")
                    .purpose("Verify complete E diagnostics without implicit loading").executeForOne(context);
            var diagnostics = new ArrayList<TeaQLNotLoadedException.WireError>();
            var error = assertThrows(TeaQLNotLoadedException.class, () -> E.purchaseOrder(partial).getTenant().getName().eval());
            assertEquals("/tenant", error.toWire().instancePath());
            assertEquals("/tenant/name", error.toWire().attemptedInstancePath());
            assertEquals("E.purchaseOrder(<root>).getTenant()<broken>.getName().eval()", error.getExpressionWithBrokenPoint());
            diagnostics.add(error.toWire());
            error = assertThrows(TeaQLNotLoadedException.class, () -> E.purchaseOrder(partial)
                    .getTenant().getPurchaseOrderList().first().getTenant().getName().eval());
            assertEquals("E.purchaseOrder(<root>).getTenant()<broken>.getPurchaseOrderList().first().getTenant().getName().eval()", error.getExpressionWithBrokenPoint());
            diagnostics.add(error.toWire());
            error = assertThrows(TeaQLNotLoadedException.class, () -> E.purchaseOrder(partial).getOrderItemList().get(2).getProductName().eval());
            assertEquals("/orderItemList", error.toWire().instancePath());
            assertEquals("/orderItemList/2/productName", error.toWire().attemptedInstancePath());
            diagnostics.add(error.toWire());
            error = assertThrows(TeaQLNotLoadedException.class, () -> E.purchaseOrder(partial).getContactPhone().orIfNull("PRIVATE-FALLBACK"));
            assertEquals("E.purchaseOrder(<root>).getContactPhone()<broken>.orIfNull(<fallback>)", error.getExpressionWithBrokenPoint());
            assertFalse(error.getMessage().contains("PRIVATE-FALLBACK"));
            diagnostics.add(error.toWire());

            time.set(second);
            var loaded = Q.purchaseOrders().withIdIs(order.getId()).limit(1)
                    .comment("Reload the complete order before changing it")
                    .purpose("Verify update Fix preserves the original create time").executeForOne(context);
            loaded.updateContactPhone("5550101");
            loaded.auditAs("Update the synthetic Fix order").save(context);
            var persisted = Q.purchaseOrders().withIdIs(order.getId()).limit(1)
                    .comment("Reload the persisted update").purpose("Verify Business Clock values using E").executeForOne(context);
            assertEquals(first, E.purchaseOrder(persisted).getCreateTime().eval());
            assertEquals(second, E.purchaseOrder(persisted).getUpdateTime().eval());
            assertEquals("5550101", E.purchaseOrder(persisted).getContactPhone().eval());
            System.out.println("FIX_E_EXAMPLE=" + new ObjectMapper().writeValueAsString(Map.of(
                    "language", "java", "passed", true, "normalGeneratedSave", true,
                    "createTimePreserved", true, "updateTimeAdvanced", true,
                    "declaredContextRootInjected", true, "diagnostics", diagnostics)));
        }
    }
}
