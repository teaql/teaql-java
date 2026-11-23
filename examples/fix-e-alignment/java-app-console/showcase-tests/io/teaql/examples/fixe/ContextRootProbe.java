package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.GeneratedRuntimeModule;
import com.teaql.checkerfixadvanced.Q;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import io.teaql.core.*;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.checker.CheckException;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Actual generated Q/E and audited SQLite saves. Generic writes are adversarial probes only. */
final class ContextRootProbe {
    static final ObjectMapper JSON = new ObjectMapper();
    static final LocalDateTime T0 = LocalDateTime.of(2032, 2, 29, 10, 15, 30);
    static final class ObservedSql extends JdbcSqlExecutor {
        int queries, writes, transactions, rootLookups;
        ObservedSql(DataSource source) { super(source); }
        void read(String sql) { queries++; if (sql.toLowerCase(Locale.ROOT).contains("tenant_data")) rootLookups++; }
        @Override public List<Map<String,Object>> queryForList(String sql,Object[] args) { read(sql); return super.queryForList(sql,args); }
        @Override public <T extends Entity> List<T> query(String sql,Object[] args,CompiledRowMapper<T> mapper) { read(sql); return super.query(sql,args,mapper); }
        @Override public int update(String sql,Object[] args) { writes++; return super.update(sql,args); }
        @Override public int[] batchUpdate(String sql,List<Object[]> args) { writes++; return super.batchUpdate(sql,args); }
        @Override public void execute(String sql) { writes++; super.execute(sql); }
        @Override public void executeInTransaction(Runnable action) { transactions++; super.executeInTransaction(action); }
    }
    static final class Fixture {
        final SQLiteDataSource raw = new SQLiteDataSource();
        final ObservedSql sql;
        final SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        final TeaQLRuntime runtime;
        final UserContext context;
        int allocations, policies, reviews, audits;
        long otherRoot;
        Fixture(String file) {
            raw.setUrl("jdbc:sqlite:"+file); sql = new ObservedSql(raw);
            var ids = new IdSpaceIdGenerator(new GeneratedFixEExampleTest.DatabaseBridge(sql)); ids.ensureIdSpaceTable();
            var service = new SqliteDataServiceExecutor("sqlite",sql,raw);
            var clock = new BusinessClock() {
                public LocalDate businessDate(UserContext context) { return T0.toLocalDate(); }
                public LocalDateTime businessDateTime(UserContext context) { return T0; }
            };
            var policy = new MutationPolicy() {
                public MutationPolicyIdentity identity() { return new MutationPolicyIdentity("context-root-probe","1","sha256:root-probe"); }
                public MutationDecision review(UserContext context,MutationPlan plan) { reviews++; return MutationDecision.allow(); }
            };
            runtime = TeaQLRuntime.builder().metadata(metadata).dataService("default",service).dataService("sqlite",service)
                    .businessClock(clock).idGenerationService(new InternalIdGenerationService() {
                        public Long generateId(UserContext caller,Entity entity) { allocations++; return ids.generateId(caller,entity); }
                        public void ensureFloor(String type,long floor) { ids.ensureFloor(type,floor); }
                    }).mutationPolicyRegistry(key -> { policies++; return Optional.of(policy); })
                    .logSink(new RuntimeLogSink() {
                        public void writeExecutionLog(UserContext caller,ExecutionMetadata event) {}
                        public void writeAuditEvent(UserContext caller,RawAuditEvent event) { audits++; }
                    }).build().install(GeneratedRuntimeModule.module());
            context = new GeneratedFixEExampleTest.Context(runtime); context.ensureSchema();
            var other = Q.tenants().comment("Prepare another actual domain root")
                    .purpose("Distinguish a real wrong owner from an invalid database reference").newEntity(context);
            other.updateProperty("name","Other root"); other.auditAs("Create the second root control").save(context); otherRoot=other.getId();
            assertNotEquals(Long.valueOf(1),other.getId());
        }
        UserContext withRoot(String type,Long id) {
            UserContext result = new GeneratedFixEExampleTest.Context(runtime);
            if(type!=null) result.withActiveRoot(new ContextEntityRef(type,id));
            return result;
        }
        PurchaseOrder parent() {
            var row=Q.purchaseOrders().comment("Prepare a root-bound owner")
                    .purpose("Verify context-root admission before persistence").newEntity(context);
            row.updateContactPhone("5550987"); row.updateInternalNote(null); return row;
        }
        PurchaseOrder saved() { return parent().auditAs("Create a root-bound admission control").save(context); }
        PurchaseOrder loaded(long id) { return Q.purchaseOrders().withIdIs(id).limit(1)
                .comment("Reload the original owner through generated Q").purpose("Retain authoritative loaded ownership").executeForOne(context); }
        void overrideRoot(PurchaseOrder row,long id) {
            // Do not discover or invoke a forbidden generated business setter.
            var reference=(BaseEntity)metadata.resolveEntityDescriptor("Tenant").createEntity();
            reference.setProperty("id",id); reference.set$status(EntityStatus.REFER);
            String member=FieldLayout.forType(row.getClass()).memberName("tenant"); assertNotNull(member);
            row.updateProperty(member,reference);
        }
        Map<String,Integer> counters() { return Map.of("queries",sql.queries,"writes",sql.writes,"transactions",sql.transactions,
                "allocations",allocations,"policies",policies,"reviews",reviews,"audits",audits,"rootLookups",sql.rootLookups); }
        Map<String,Integer> delta(Map<String,Integer> before) { var values=new TreeMap<String,Integer>(); counters().forEach((k,v)->values.put(k,v-before.get(k))); return values; }
        List<List<Object>> storage() throws Exception {
            var rows=new ArrayList<List<Object>>();
            try(var connection=raw.getConnection();var statement=connection.createStatement();var result=statement.executeQuery(
                    "SELECT id,version,tenant,contact_phone FROM purchase_order_data ORDER BY id")) {
                while(result.next()) rows.add(Arrays.asList(result.getLong(1),result.getLong(2),result.getObject(3),result.getString(4)));
            }
            return rows;
        }
    }
    static Map<String,Object> reject(Fixture f,String name,PurchaseOrder row,UserContext context,String reason) throws Exception {
        var before=f.counters(); var stored=f.storage(); String actual=null,errorType=null;
        try { row.auditAs("Reject "+name+" before persistence").save(context); }
        catch(ContextRootException error) { actual=error.getReason().name(); errorType="ContextRootException"; }
        catch(CheckException error) { actual=error.getViolates().isEmpty()?"empty_check":error.getViolates().get(0).toWire(io.teaql.core.checker.JsonFieldNamingProfile.CAMEL_CASE).ruleId(); errorType="CheckException"; }
        catch(RuntimeException error) { actual="provider_or_other"; errorType=error.getClass().getSimpleName(); }
        var effects=f.delta(before); boolean unchanged=stored.equals(f.storage());
        boolean matched=reason.equals(actual);
        var result=new LinkedHashMap<String,Object>();result.put("case",name);result.put("expectedReason",reason);result.put("reason",actual);
        result.put("errorType",errorType);result.put("id",row.getId());result.put("effects",effects);result.put("physicalStateUnchanged",unchanged);
        result.put("passed",matched&&unchanged&&effects.values().stream().allMatch(v->v==0));
        System.out.println("JAVA_CONTEXT_ROOT_CASE="+JSON.writeValueAsString(result));return result;
    }
    static void verify() throws Exception {
        var f=new Fixture(System.getenv("TEAQL_FIX_E_DB")+".context-root.sqlite"); var cases=new ArrayList<Map<String,Object>>();
        var owner=f.parent();var item=Q.orderItems().comment("Prepare a child in the same root-bound graph")
                .purpose("Verify parent and child receive a trusted root reference").newEntity(f.context);
        item.updatePurchaseOrder(owner);item.updateProductName("Root control item");item.updateUnitPrice(BigDecimal.ONE);
        var before=f.counters();item.auditAs("Save the trusted parent and child graph").save(f.context);var calibration=f.delta(before);
        assertEquals(Long.valueOf(1),E.purchaseOrder(owner).getTenant().getId().eval());
        assertEquals(Long.valueOf(1),E.orderItem(item).getTenant().getId().eval());
        assertTrue(calibration.get("writes")>0);assertTrue(calibration.get("transactions")>0);assertTrue(calibration.get("policies")>0);
        assertTrue(calibration.get("reviews")>0);assertEquals(Integer.valueOf(2),calibration.get("audits"));assertEquals(Integer.valueOf(0),calibration.get("rootLookups"));
        before=f.counters(); Q.tenants().withIdIs(1L).limit(1).comment("Calibrate the root lookup observer")
                .purpose("Ensure a missing root lookup is measured rather than assumed").executeForOne(f.context);
        var rootCalibration=f.delta(before);assertTrue(rootCalibration.get("rootLookups")>0);
        for(var entry:List.of(new Object[]{"missing-root",null,null,"MISSING"},new Object[]{"wrong-type","Other",1L,"TYPE_MISMATCH"},
                new Object[]{"zero-root","Tenant",0L,"INVALID_ID"},new Object[]{"negative-root","Tenant",-1L,"INVALID_ID"}))
            cases.add(reject(f,(String)entry[0],f.parent(),f.withRoot((String)entry[1],(Long)entry[2]),(String)entry[3]));
        var bad=f.parent();f.overrideRoot(bad,f.otherRoot);cases.add(reject(f,"explicit-create-mismatch",bad,f.context,"VALUE_MISMATCH"));
        for(String name:List.of("update-wrong-context","update-payload-mismatch","update-context-and-root","update-missing-root",
                "delete-wrong-context","delete-missing-root","recover-wrong-context","recover-missing-root")) {
            var original=f.saved();var row=f.loaded(original.getId());
            if(name.startsWith("recover")) {row.markForDeletion();row.auditAs("Prepare a real tombstone for recovery admission").save(f.context);row.markAsRecover();}
            else if(name.startsWith("delete"))row.markForDeletion();
            else row.updateContactPhone("5550988");
            if(name.equals("update-payload-mismatch")||name.equals("update-context-and-root"))f.overrideRoot(row,f.otherRoot);
            boolean missing=name.endsWith("missing-root"),same=name.equals("update-payload-mismatch");
            var caller=missing?f.withRoot(null,null):same?f.context:f.withRoot("Tenant",f.otherRoot);
            cases.add(reject(f,name,row,caller,missing?"MISSING":"VALUE_MISMATCH"));
        }
        var failures=cases.stream().filter(v->!Boolean.TRUE.equals(v.get("passed"))).map(v->v.get("case")).toList();
        var result=new LinkedHashMap<String,Object>();result.put("language","java");result.put("passed",failures.isEmpty());result.put("cases",cases);
        result.put("failures",failures);result.put("parentId",owner.getId());result.put("childId",item.getId());result.put("otherRoot",f.otherRoot);
        result.put("calibration",calibration);result.put("rootCalibration",rootCalibration);result.put("finalStorage",f.storage());
        result.put("queryAuthorizationInferred",false);result.put("newEntityBeforeReturnAccepted",false);result.put("untrustedHydrationAuthorizationInferred",false);
        System.out.println("JAVA_CONTEXT_ROOT_RESULT="+JSON.writeValueAsString(result));assertTrue("root admission failures: "+failures,failures.isEmpty());
    }
}
