package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.GeneratedRuntimeModule;
import com.teaql.checkerfixadvanced.Q;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import io.teaql.core.*;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.runtime.RuntimeLogSink;
import io.teaql.runtime.AppAuditEventSink;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.sqlite.Function;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Original ML-01..07, without substituting list-result isolation for two queries. */
public class OriginalLedgerExampleTest {
    static final class WatchedSource extends SQLiteDataSource {
        final AtomicInteger trips = new AtomicInteger();
        @Override public Connection getConnection() throws SQLException {
            var connection = super.getConnection();
            Function.create(connection, "core236_ml07", new Function() {
                protected void xFunc() throws SQLException { trips.incrementAndGet(); result(1); }
            });
            return connection;
        }
    }
    static PurchaseOrder owner(UserContext context, String phone) {
        var value = Q.purchaseOrders().comment("Prepare an independent ledger owner")
                .purpose("Verify original mutation ownership clauses").newEntity(context);
        value.updateContactPhone(phone); value.updateInternalNote(null); return value;
    }
    static PurchaseOrder load(UserContext context, long id) {
        return Q.purchaseOrders().withIdIs(id).limit(1).comment("Load one independent ledger snapshot")
                .purpose("Verify separate queries retain separate pending intent").executeForOne(context);
    }
    static void emit(String clause, int round, Map<String,Object> details) throws Exception {
        System.out.println("JAVA_ORIGINAL_LEDGER_CASE=" + new ObjectMapper().writeValueAsString(
                Map.of("case", clause, "round", round, "passed", true, "details", details)));
    }
    static void origins() throws Exception {
        var expected = Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        var result = new TreeMap<String,Object>();
        for (var type : List.of(UserContext.class, EntityMutationLedger.class, TeaQLRuntime.class,
                SqliteDataServiceExecutor.class, JdbcSqlExecutor.class, IdSpaceIdGenerator.class)) {
            var origin = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            assertTrue(type.getName() + " must use the admitted runtime", origin.startsWith(expected));
            var entry = type.getName().replace('.', '/') + ".class";
            try (var input = type.getResourceAsStream("/" + entry)) {
                assertNotNull(input);
                result.put(entry, Map.of("origin", origin.toString(), "sha256",
                        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()))));
            }
        }
        System.out.println("JAVA_ORIGINAL_LEDGER_ORIGINS=" + new ObjectMapper().writeValueAsString(result));
    }
    @Test public void originalSevenClausesUseIndependentGeneratedRootsAndRetryTheSameObject() throws Exception {
        origins(); int round = Integer.parseInt(System.getenv("TEAQL_FIX_E_ROUND"));
        var source = new WatchedSource(); source.setUrl("jdbc:sqlite:" + System.getenv("TEAQL_FIX_E_DB") + ".original-ledger.sqlite");
        var sql = new JdbcSqlExecutor(source); var ids = new IdSpaceIdGenerator(new GeneratedFixEExampleTest.DatabaseBridge(sql));
        ids.ensureIdSpaceTable(); var service = new SqliteDataServiceExecutor("sqlite", sql, source);
        var audits = new AtomicInteger(); var plans = new ArrayList<MutationPlan>();
        var policy = new MutationPolicy() {
            public MutationPolicyIdentity identity() { return new MutationPolicyIdentity("original-ledger", "1", "ML01-07"); }
            public MutationDecision review(UserContext caller, MutationPlan plan) { plans.add(plan); return MutationDecision.allow(); }
        };
        var now = LocalDateTime.of(2032, 5, 6, 7, 8, 9);
        var clock = new BusinessClock() {
            public LocalDate businessDate(UserContext caller) { return now.toLocalDate(); }
            public LocalDateTime businessDateTime(UserContext caller) { return now; }
        };
        var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .dataService("default", service).dataService("sqlite", service).idGenerationService(ids)
                .businessClock(clock).executionLogging(false).mutationPolicyRegistry(key -> Optional.of(policy))
                .logSink(new RuntimeLogSink() { public void writeExecutionLog(UserContext caller, ExecutionMetadata event) {} })
                .build().install(GeneratedRuntimeModule.module());
        UserContext context = new GeneratedFixEExampleTest.Context(runtime);
        context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink)(caller,event)->audits.incrementAndGet());
        context.ensureSchema();
        try (var c = source.getConnection(); var s = c.createStatement(); var rows = s.executeQuery("SELECT count(*) FROM purchase_order_data")) {
            assertTrue(rows.next()); assertEquals((round-1)*2, rows.getInt(1));
        }

        var a = owner(context,"5551000"); var b = owner(context,"5552000");
        long aid=a.getId(),bid=b.getId(); assertTrue(aid>0&&bid>0); assertNotEquals(aid,bid);
        var ak=new EntityKey("PurchaseOrder",aid); var bk=new EntityKey("PurchaseOrder",bid);
        assertNotSame(a.getEntityMutationLedger(),b.getEntityMutationLedger());
        assertTrue(a.getEntityMutationLedger().newKeys().contains(ak)); assertFalse(a.getEntityMutationLedger().newKeys().contains(bk));
        assertTrue(b.getEntityMutationLedger().newKeys().contains(bk)); assertNull(a.getEntityMutationLedger().get(bk,"contact_phone"));
        assertNull(b.getEntityMutationLedger().get(ak,"contact_phone"));
        emit("ML-01",round,Map.of("a",aid,"b",bid,"distinctPendingKeys",true));
        a.auditAs("Save only the first independent root").save(context);
        assertNull(load(context,bid)); assertEquals(Long.valueOf(1),a.getVersion());
        emit("ML-03",round,Map.of("a",aid,"b",bid,"unrelatedRowsWritten",0));
        assertTrue(b.getEntityMutationLedger().newKeys().contains(bk)); assertEquals("5552000",b.getEntityMutationLedger().get(bk,"contact_phone"));
        assertTrue(a.getEntityMutationLedger().newKeys().isEmpty()); assertTrue(a.getEntityMutationLedger().changedFieldNames(ak).isEmpty());
        emit("ML-06",round,Map.of("a",aid,"b",bid,"committedRootClear",true,"otherRootPending",true));
        b.auditAs("Save the still-pending second root").save(context); assertEquals(Long.valueOf(bid),b.getId()); assertEquals(Long.valueOf(1),b.getVersion());
        assertEquals("5552000",E.purchaseOrder(load(context,bid)).getContactPhone().eval()); assertTrue(b.getEntityMutationLedger().newKeys().isEmpty());
        emit("ML-04",round,Map.of("b",bid,"reservedIdRetained",true,"version",1));

        var left=load(context,aid); var right=load(context,aid); assertNotSame(left,right);
        left.updateContactPhone("5551001"); right.updateContactPhone("5551002");
        assertNotSame(left.getEntityMutationLedger(),right.getEntityMutationLedger());
        assertEquals("5551001",left.getEntityMutationLedger().get(ak,"contact_phone"));
        assertEquals("5551002",right.getEntityMutationLedger().get(ak,"contact_phone"));
        left.auditAs("Save the first independently queried snapshot").save(context);
        assertEquals(Long.valueOf(2),left.getVersion()); assertEquals(Long.valueOf(1),right.getVersion());
        assertEquals(Long.valueOf(1),right.getEntityMutationLedger().getOriginalVersion(ak));
        assertEquals("5551002",right.getEntityMutationLedger().get(ak,"contact_phone"));
        assertTrue(left.getEntityMutationLedger().changedFieldNames(ak).isEmpty());
        assertEquals("5551001",E.purchaseOrder(load(context,aid)).getContactPhone().eval());
        emit("ML-02",round,Map.of("id",aid,"otherIntentRetained",true,"otherOriginalVersion",1,"savedVersion",2));

        var composed=Q.purchaseOrders().withIdIs(aid).limit(1)
                .selectOrderItemListWith(Q.orderItems().limit(10).comment("Load the owner's bounded child list"))
                .comment("Load a complete owner for explicit child attachment")
                .purpose("Verify pending-intent composition without unrelated writes").executeForOne(context);
        var item=Q.orderItems().comment("Prepare an independently pending child")
                .purpose("Verify explicit pending-intent composition").newEntity(context);
        item.updateProductName("ML05 child"); item.updateUnitPrice(BigDecimal.TEN); long cid=item.getId();
        var ck=new EntityKey("OrderItem",cid); assertFalse(composed.getEntityMutationLedger().newKeys().contains(ck));
        composed.addOrderItem(item);
        assertEquals(Long.valueOf(cid),E.purchaseOrder(composed).getOrderItemList().get(0).getId().eval());
        // Java merges reachable entity ledgers during Save preparation, not add.
        // Verify the reviewed business plan and committed graph, not eager storage layout.
        int beforeAttachmentAudits=audits.get(); plans.clear();
        composed.auditAs("Persist an explicitly composed child through its owner").save(context);
        assertEquals(1,plans.size()); assertEquals(1,plans.get(0).operations().size());
        var attachedOperation=plans.get(0).operations().get(0);
        assertEquals(ck,attachedOperation.entity()); assertEquals(MutationOperationKind.CREATE,attachedOperation.kind());
        assertNull(attachedOperation.originalVersion()); assertEquals(beforeAttachmentAudits+1,audits.get());
        assertSame(composed.getEntityMutationLedger(),item.getEntityMutationLedger());
        assertTrue(item.getEntityMutationLedger().newKeys().isEmpty());
        assertEquals(Long.valueOf(2),composed.getVersion()); assertEquals(Long.valueOf(1),item.getVersion());
        var loadedItem=Q.orderItems().withIdIs(cid).limit(1).comment("Read the explicitly composed child")
                .purpose("Verify owner save persisted child intent").executeForOne(context);
        assertNotNull(loadedItem); assertEquals("ML05 child",E.orderItem(loadedItem).getProductName().eval());
        assertEquals(Long.valueOf(1),E.orderItem(loadedItem).getVersion().eval());
        assertEquals(0,BigDecimal.TEN.compareTo(E.orderItem(loadedItem).getUnitPrice().eval()));
        assertTrue(composed.getEntityMutationLedger().newKeys().isEmpty()); assertTrue(composed.getEntityMutationLedger().changedFieldNames(ck).isEmpty());
        emit("ML-05",round,Map.of("a",aid,"child",cid,"childVersion",1,"ownerVersion",2,"receivingLedgerClear",true));

        var failed=load(context,aid); var unrelated=load(context,bid);
        failed.updateContactPhone("5551003"); unrelated.updateContactPhone("5552001");
        try (var c=source.getConnection(); var s=c.createStatement()) {
            s.execute("CREATE TRIGGER core236_ml07_failure BEFORE UPDATE ON purchase_order_data WHEN NEW.id="+aid+" AND NEW.contact_phone='5551003' BEGIN SELECT core236_ml07(); SELECT RAISE(ABORT,'CORE236_ML07'); END");
        }
        audits.set(0); plans.clear();
        var error=assertThrows(RuntimeException.class,()->failed.auditAs("Reject one root without altering the other root").save(context));
        boolean triggerError=false; for(Throwable cause=error;cause!=null;cause=cause.getCause())if(String.valueOf(cause.getMessage()).contains("CORE236_ML07"))triggerError=true;
        assertTrue(triggerError); assertEquals(1,source.trips.get()); assertEquals(0,audits.get());
        assertEquals("5551003",failed.getEntityMutationLedger().get(ak,"contact_phone")); assertEquals(Long.valueOf(2),failed.getEntityMutationLedger().getOriginalVersion(ak));
        assertEquals("5552001",unrelated.getEntityMutationLedger().get(bk,"contact_phone")); assertEquals(Long.valueOf(1),unrelated.getEntityMutationLedger().getOriginalVersion(bk));
        assertEquals("5551001",E.purchaseOrder(load(context,aid)).getContactPhone().eval()); assertEquals(Long.valueOf(2),load(context,aid).getVersion());
        unrelated.auditAs("Commit the unrelated root while A remains failed").save(context); assertEquals(Long.valueOf(2),unrelated.getVersion());
        assertEquals("5551003",failed.getEntityMutationLedger().get(ak,"contact_phone"));
        try(var c=source.getConnection();var s=c.createStatement()){s.execute("DROP TRIGGER core236_ml07_failure");}
        failed.auditAs("Retry the same failed root without affecting B").save(context); assertEquals(Long.valueOf(3),failed.getVersion());
        assertTrue(failed.getEntityMutationLedger().changedFieldNames(ak).isEmpty()); assertTrue(unrelated.getEntityMutationLedger().changedFieldNames(bk).isEmpty());
        assertEquals("5552001",E.purchaseOrder(load(context,bid)).getContactPhone().eval()); assertEquals(2,audits.get());
        emit("ML-07",round,Map.of("a",aid,"b",bid,"auditsAfterFailure",0,"finalA",3,"finalB",2,"retrySameRoot",true,"triggerTrips",1));
    }
}
