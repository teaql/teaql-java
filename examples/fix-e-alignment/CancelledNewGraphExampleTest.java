package io.teaql.examples.fixe;

import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.GeneratedRuntimeModule;
import com.teaql.checkerfixadvanced.Q;
import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import io.teaql.core.*;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.checker.CheckException;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.runtime.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** ML-12 through the retained generated model, Checker/Fix, Q/E and real SQLite. */
public class CancelledNewGraphExampleTest {
    private static final class Fixture {
        final SQLiteDataSource source = new SQLiteDataSource();
        final ContextRootProbe.ObservedSql sql;
        final UserContext context;
        final int round = Integer.parseInt(System.getenv("TEAQL_FIX_E_ROUND"));
        int mutations, audits;
        boolean armed;

        Fixture(String scenario) {
            String base = System.getenv("TEAQL_FIX_E_DB"); assertNotNull(base);
            source.setUrl("jdbc:sqlite:" + base + ".cancelled-new-" + scenario + ".sqlite");
            sql = new ContextRootProbe.ObservedSql(source);
            var ids = new IdSpaceIdGenerator(new GeneratedFixEExampleTest.DatabaseBridge(sql));
            ids.ensureIdSpaceTable();
            var service = new SqliteDataServiceExecutor("sqlite", sql, source) {
                @Override public MutationResult mutate(UserContext caller, PersistenceMutation request) {
                    if (armed) mutations++;
                    return super.mutate(caller, request);
                }
            };
            BusinessClock clock = new BusinessClock() {
                public LocalDate businessDate(UserContext caller) { return businessDateTime(caller).toLocalDate(); }
                public LocalDateTime businessDateTime(UserContext caller) { return LocalDateTime.of(2032, 2, 29, 10, 15, 30).plusDays(round - 1); }
            };
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default", service).dataService("sqlite", service)
                    .idGenerationService(ids).businessClock(clock).executionLogging(false)
                    .build().install(GeneratedRuntimeModule.module());
            context = new GeneratedFixEExampleTest.Context(runtime);
            context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> { if (armed) audits++; });
            context.ensureSchema();
        }

        PurchaseOrder parent() {
            var entity = Q.purchaseOrders().comment("Prepare a cancellable graph")
                    .purpose("Verify generated cancellation semantics before persistence").newEntity(context);
            entity.updateContactPhone("5550199"); return entity;
        }
        OrderItem child(boolean complete) {
            var entity = Q.orderItems().comment("Prepare a newly reserved child")
                    .purpose("Verify incomplete and complete new children can be cancelled").newEntity(context);
            if (complete) { entity.updateProductName("kept child"); entity.updateUnitPrice(BigDecimal.ONE); }
            return entity;
        }
        void cancel(BaseEntity entity) {
            int reads = sql.queries, writes = sql.writes;
            entity.markForDeletion();
            assertEquals(reads, sql.queries); assertEquals(writes, sql.writes);
            assertTrue(entity.isNew()); assertTrue(entity.isMarkedAsDelete());
        }
        void absent(OrderItem entity) {
            assertNull(Q.orderItems().withIdIs(entity.getId()).limit(1)
                    .comment("Verify the cancelled child is not stored")
                    .purpose("A never-inserted child must not leave a row or tombstone").executeForOne(context));
            var rows = sql.queryForList("SELECT id FROM order_item_data WHERE id=?", new Object[]{entity.getId()});
            assertTrue(rows.isEmpty()); assertNull(entity.getVersion());
        }
        void emit(String scenario) throws Exception {
            System.out.println("JAVA_CANCELLED_NEW_GRAPH_CASE=" + ContextRootProbe.JSON.writeValueAsString(Map.of(
                    "case", scenario, "round", round, "passed", true, "generatedQAndE", true,
                    "mutations", mutations, "audits", audits, "cancelledRowAbsent", true)));
        }
    }

    @Test public void generatedCancellationKeepsOnlyIntendedSQLiteMutations() throws Exception {
        var rootCase = new Fixture("root"); var root = rootCase.parent(); rootCase.cancel(root); rootCase.armed = true;
        int writes = rootCase.sql.writes;
        var error = assertThrows(TeaQLRuntimeException.class,
                () -> root.auditAs("reject a cancelled new root at save").save(rootCase.context));
        assertTrue(error.getMessage().contains("CANCELLED_NEW_ROOT"));
        assertEquals(writes, rootCase.sql.writes); assertEquals(0, rootCase.mutations); assertEquals(0, rootCase.audits);
        assertNull(root.getVersion());
        assertNull(Q.purchaseOrders().withIdIs(root.getId()).limit(1).comment("Verify cancelled root absence")
                .purpose("No persisted entity exists for a cancelled creation").executeForOne(rootCase.context));
        assertTrue(rootCase.sql.queryForList("SELECT id FROM purchase_order_data WHERE id=?", new Object[]{root.getId()}).isEmpty());
        rootCase.emit("root");

        for (String scenario : new String[]{"complete-child", "incomplete-child", "composed-child"}) {
            var f = new Fixture(scenario); var parent = f.parent();
            var cancelled = f.child(!scenario.equals("incomplete-child")); var live = f.child(true);
            if (scenario.equals("composed-child")) parent.addOrderItem(cancelled);
            f.cancel(cancelled);
            if (!scenario.equals("composed-child")) parent.addOrderItem(cancelled);
            parent.addOrderItem(live); f.armed = true;
            parent.auditAs("save parent and live sibling without cancelled new child").save(f.context);
            assertEquals(2, f.mutations); assertEquals(2, f.audits); f.absent(cancelled);
            var loaded = Q.purchaseOrders().withIdIs(parent.getId()).limit(1).comment("Reload saved parent")
                    .purpose("Verify generated Q/E after omitting a cancelled child").executeForOne(f.context);
            assertEquals("5550199", E.purchaseOrder(loaded).getContactPhone().eval());
            var child = Q.orderItems().withIdIs(live.getId()).limit(1).comment("Reload intended live child")
                    .purpose("Cancelling a sibling must not suppress the live mutation").executeForOne(f.context);
            assertEquals("kept child", E.orderItem(child).getProductName().eval());
            assertEquals(Long.valueOf(1), E.orderItem(child).getVersion().eval());
            var retry = assertThrows(TeaQLRuntimeException.class,
                    () -> cancelled.auditAs("a cancelled child cannot be resurrected after parent save").save(f.context));
            assertTrue(retry.getMessage().contains("CANCELLED_NEW_ROOT")); assertEquals(2, f.mutations);
            f.emit(scenario);
        }

        var f = new Fixture("invalid-live-sibling"); var parent = f.parent();
        var cancelled = f.child(false); var invalid = f.child(false); f.cancel(cancelled);
        parent.addOrderItem(cancelled); parent.addOrderItem(invalid); f.armed = true;
        assertThrows(CheckException.class, () -> parent.auditAs("live sibling still requires complete valid data").save(f.context));
        assertEquals(0, f.mutations); assertEquals(0, f.audits); f.absent(cancelled); f.emit("invalid-live-sibling");
    }
}
