package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.GeneratedRuntimeModule;
import com.teaql.checkerfixadvanced.Q;
import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import io.teaql.core.*;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.checker.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import io.teaql.tfp.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import javax.sql.DataSource;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Real generated Q/E/audited saves plus existing public Checker and JSON SPIs. */
public class CheckerBoundaryExampleTest {
    static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
    static final LocalDateTime CLOCK = LocalDateTime.of(2032, 2, 29, 10, 15, 30);

    static final class Observed extends JdbcSqlExecutor {
        int queries, writes, transactions, commits, parentInserts, childInserts;
        Observed(DataSource source) { super(source); }
        void write(String sql, int count) {
            writes++;
            String s = sql.toLowerCase(Locale.ROOT).replace("\"", "").trim();
            if (s.startsWith("insert into purchase_order_data")) parentInserts += count;
            if (s.startsWith("insert into order_item_data")) childInserts += count;
        }
        @Override public List<Map<String,Object>> queryForList(String sql, Object[] args) { queries++; return super.queryForList(sql,args); }
        @Override public <T extends Entity> List<T> query(String sql,Object[] args,CompiledRowMapper<T> mapper) { queries++; return super.query(sql,args,mapper); }
        @Override public int update(String sql,Object[] args) { write(sql,1); return super.update(sql,args); }
        @Override public int[] batchUpdate(String sql,List<Object[]> args) { write(sql,args.size()); return super.batchUpdate(sql,args); }
        @Override public void execute(String sql) { write(sql,1); super.execute(sql); }
        @Override public void executeInTransaction(Runnable action) { transactions++; super.executeInTransaction(action); commits++; }
    }
    static final class Fixture {
        final SQLiteDataSource raw = new SQLiteDataSource();
        final Observed sql;
        final UserContext context;
        int allocations, policies, reviews, audits, checkers, mapped, saves, round, observations;
        String mode = "";
        @SuppressWarnings("unchecked") Fixture() throws Exception {
            String base = System.getenv("TEAQL_FIX_E_DB"); assertNotNull(base);
            raw.setUrl("jdbc:sqlite:"+base+".checker-boundaries.sqlite"); sql = new Observed(raw);
            var ids = new IdSpaceIdGenerator(new GeneratedFixEExampleTest.DatabaseBridge(sql)); ids.ensureIdSpaceTable();
            var service = new SqliteDataServiceExecutor("sqlite",sql,raw);
            var clock = new BusinessClock() {
                public LocalDate businessDate(UserContext caller) { return CLOCK.toLocalDate(); }
                public LocalDateTime businessDateTime(UserContext caller) { return CLOCK; }
            };
            var policy = new MutationPolicy() {
                public MutationPolicyIdentity identity() { return new MutationPolicyIdentity("checker-boundary","1","sha256:checker-boundary"); }
                public MutationDecision review(UserContext caller,MutationPlan plan) { reviews++; return MutationDecision.allow(); }
            };
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory()).dataService("default",service).dataService("sqlite",service)
                    .businessClock(clock).idGenerationService(new InternalIdGenerationService() {
                        public Long generateId(UserContext caller,Entity entity) { allocations++; return ids.generateId(caller,entity); }
                        public void ensureFloor(String type,long floor) { ids.ensureFloor(type,floor); }
                    }).mutationPolicyRegistry(key -> { policies++; return Optional.of(policy); })
                    .logSink(new RuntimeLogSink() {
                        public void writeExecutionLog(UserContext caller,ExecutionMetadata event) {}
                        public void writeAuditEvent(UserContext caller,RawAuditEvent event) { audits++; }
                    }).build();
            var module = GeneratedRuntimeModule.module(); runtime.install(module);
            var decorated = RuntimeModule.of();
            for (Checker<?> original : module.checkers()) {
                Checker<BaseEntity> delegate = (Checker<BaseEntity>)original;
                decorated = decorated.withCheckers(new Checker<BaseEntity>() {
                    public String type() { return delegate.type(); }
                    public boolean needCheck(UserContext caller,BaseEntity entity) { return delegate.needCheck(caller,entity); }
                    public void checkAndFix(UserContext caller,BaseEntity entity,ObjectLocation location) {
                        checkers++;
                        if (entity instanceof OrderItem item && item.newItem()) {
                            if (mode.equals("reject")) item.updateUnitPrice(BigDecimal.ZERO);
                            if (mode.equals("repair")) { item.updateProductName("Fixed child"); item.updateUnitPrice(new BigDecimal("12.34")); }
                        }
                        delegate.checkAndFix(caller,entity,location);
                    }
                });
            }
            runtime.install(decorated); context = new GeneratedFixEExampleTest.Context(runtime); context.ensureSchema();
            int count = physical().get("purchase_order_data").size(); assertEquals(0,count%5); round = count/5+1;
            assertTrue("only two unreset rounds",round==1||round==2);
        }
        PurchaseOrder parent() {
            var row = Q.purchaseOrders().comment("Prepare a Java Checker boundary")
                    .purpose("Validate generated saves before provider entry").newEntity(context);
            row.updateInternalNote(null); return row;
        }
        OrderItem item(PurchaseOrder parent,String name) {
            var row = Q.orderItems().comment("Prepare a real child constraint")
                    .purpose("Exercise the public Checker SPI through audited saves").newEntity(context);
            row.updatePurchaseOrder(parent); row.updateProductName(name); row.updateUnitPrice(new BigDecimal("10.00")); return row;
        }
        Map<String,Integer> counters() {
            var r = new TreeMap<String,Integer>(); r.put("queries",sql.queries);r.put("writes",sql.writes);r.put("transactions",sql.transactions);
            r.put("commits",sql.commits);r.put("parentInserts",sql.parentInserts);r.put("childInserts",sql.childInserts);
            r.put("allocations",allocations);r.put("policies",policies);r.put("reviews",reviews);r.put("audits",audits);
            r.put("checkers",checkers);r.put("mapped",mapped);r.put("saves",saves);return r;
        }
        Map<String,Integer> delta(Map<String,Integer> before) { var r = new TreeMap<String,Integer>(); counters().forEach((k,v)->r.put(k,v-before.get(k)));return r; }
        void zero(Map<String,Integer> effects,boolean allowChecker) {
            for (var e:effects.entrySet()) if (!Set.of("mapped","saves").contains(e.getKey()) && !(allowChecker&&e.getKey().equals("checkers")))
                assertEquals(effects.toString(),Integer.valueOf(0),e.getValue());
        }
        Map<String,List<Map<String,Object>>> physical() throws Exception {
            var all = new TreeMap<String,List<Map<String,Object>>>();
            try (var connection = raw.getConnection(); var statement = connection.createStatement()) {
                for (String table:List.of("tenant_data","purchase_order_data","order_item_data")) {
                    var rows = new ArrayList<Map<String,Object>>();
                    try(var result=statement.executeQuery("SELECT * FROM "+table+" ORDER BY id")) {
                        while(result.next()) { var row=new TreeMap<String,Object>();var meta=result.getMetaData();
                            for(int i=1;i<=meta.getColumnCount();i++) row.put(meta.getColumnName(i),
                                    meta.getColumnName(i).equals("unit_price")?result.getString(i):result.getObject(i)); rows.add(row); }
                    }
                    all.put(table,rows);
                }
            }
            return all;
        }
        void emit(String name,Map<String,?> facts,String profile) throws Exception {
            var row=new LinkedHashMap<String,Object>(facts);row.put("case",name);row.put("round",round);row.put("passed",true);
            if(profile!=null)row.put("profile",profile);System.out.println("JAVA_CHECKER_BOUNDARY_CASE="+JSON.writeValueAsString(row));observations++;
        }
        CheckResult reject(String rule,String location,ThrowingRunnable save) throws Exception {
            var stored=physical();var before=counters();var error=assertThrows(CheckException.class,save);
            zero(delta(before),true);assertEquals(stored,physical());
            return error.getViolates().stream().filter(v->v.toWire(JsonFieldNamingProfile.SNAKE_CASE).ruleId().equals(rule)
                    &&v.getLocation().modelPath().equals(location)).findFirst().orElseThrow(()->new AssertionError(error.getViolates()));
        }
    }

    @Test public void realGeneratedGraphAndJsonInputBoundaries() throws Exception {
        var f=new Fixture();
        var parent=f.parent();parent.updateContactPhone("5550100");var child=f.item(parent,"Boundary item");
        var before=f.counters();child.auditAs("Calibrate exactly-once generated graph persistence").save(f.context);var effects=f.delta(before);
        assertEquals(Integer.valueOf(1),effects.get("parentInserts"));assertEquals(Integer.valueOf(1),effects.get("childInserts"));
        assertEquals(Integer.valueOf(2),effects.get("audits"));assertTrue(effects.get("checkers")>0);assertTrue(effects.get("transactions")>0);
        f.emit("exact-provider-calibration",Map.of("parentId",parent.getId(),"childId",child.getId(),"effects",effects),null);

        var root=Q.tenantsWithMinimalFields().withIdIs(1L).selectPurchaseOrderListWith(
                Q.purchaseOrdersWithMinimalFields().withIdIs(parent.getId()).selectTenant().selectContactPhone().selectCreateTime().selectUpdateTime()
                        .limit(1).comment("Load every fixed field except the optional note"))
                .limit(1).comment("Load an actually incomplete modified object beneath a sparse root")
                .purpose("Distinguish optional-field completeness from missing-root admission").executeForOne(f.context);
        var partial=E.tenant(root).getPurchaseOrderList().first().eval();
        assertEquals(Long.valueOf(1),E.purchaseOrder(partial).getTenant().getId().eval());
        assertThrows(TeaQLNotLoadedException.class,()->E.purchaseOrder(partial).getInternalNote().eval());
        partial.updateContactPhone("5550101");
        var violation=f.reject("invalid_type","purchase_order_list[0].internal_note",
                ()->root.auditAs("Reject the actual incomplete mutation target").save(f.context));
        assertEquals("PurchaseOrder",violation.toWire(JsonFieldNamingProfile.CAMEL_CASE).entityType());
        f.emit("nonroot-partial-update",Map.of("loadedRoot",1,"absentField","internal_note",
                "nativePath",violation.getLocation().nativePath(),"zeroProviderIO",true),null);

        var completeRoot=Q.tenantsWithMinimalFields().withIdIs(1L).selectPurchaseOrderListWith(Q.purchaseOrders().withIdIs(parent.getId())
                        .limit(1).comment("Load the complete actual mutation target"))
                .limit(1).comment("Keep the untouched ancestor sparse").purpose("Save only entities with actual changes").executeForOne(f.context);
        assertThrows(TeaQLNotLoadedException.class,()->E.tenant(completeRoot).getName().eval());
        var complete=E.tenant(completeRoot).getPurchaseOrderList().first().eval();complete.updateContactPhone("5550102");
        before=f.counters();completeRoot.auditAs("Save the complete child under a sparse untouched root").save(f.context);effects=f.delta(before);
        assertEquals(Long.valueOf(1),E.tenant(completeRoot).getVersion().eval());assertEquals(Long.valueOf(2),E.purchaseOrder(complete).getVersion().eval());
        assertEquals(Integer.valueOf(1),effects.get("audits"));assertEquals(Integer.valueOf(0),effects.get("parentInserts"));
        f.emit("unmodified-partial-ancestor",Map.of("parentId",parent.getId(),"childVersion",2,"rootVersion",1,"effects",effects),null);

        var rejected=f.parent();rejected.updateContactPhone("5550100");var rejectedChild=f.item(rejected,"Boundary item");f.mode="reject";
        violation=f.reject("min","unit_price",()->rejectedChild.auditAs("Reject a range violation introduced by customer Fix").save(f.context));
        assertEquals("OrderItem",violation.toWire(JsonFieldNamingProfile.CAMEL_CASE).entityType());
        assertEquals(0,BigDecimal.ZERO.compareTo(E.orderItem(rejectedChild).getUnitPrice().eval()));
        f.emit("post-fix-rejection",Map.of("originalPrice","10.00","fixedPrice","0","actualFixReached",true,"zeroProviderIO",true),null);
        var repaired=f.parent();repaired.updateContactPhone("5550100");var repairedChild=f.item(repaired,null);f.mode="repair";
        before=f.counters();repairedChild.auditAs("Let customer Fix supply a required name and exact Decimal").save(f.context);effects=f.delta(before);f.mode="";
        var stored=Q.orderItems().withIdIs(repairedChild.getId()).limit(1).comment("Read actual post-Fix values")
                .purpose("Detect stale mutation compilation after the Checker SPI").executeForOne(f.context);
        assertEquals("Fixed child",E.orderItem(stored).getProductName().eval());assertEquals(0,new BigDecimal("12.34").compareTo(E.orderItem(stored).getUnitPrice().eval()));
        assertEquals(Integer.valueOf(1),effects.get("parentInserts"));assertEquals(Integer.valueOf(1),effects.get("childInserts"));assertEquals(Integer.valueOf(2),effects.get("audits"));
        f.emit("post-fix-repair",Map.of("parentId",repaired.getId(),"childId",repairedChild.getId(),"storedName","Fixed child","storedPrice","12.34","effects",effects),null);

        for(var profile:JsonFieldNamingProfile.values()) {
            String name=profile.modelValue(),standard=profile.render("contact_phone");
            var metadata=new WireEntityMetadata("PurchaseOrder",Map.of("contact_phone",standard),Map.of("legacy/phone~","contact_phone"));
            // Java reserves the ID when newEntity returns. Do that before the
            // measured input/save boundary; never mislabel ID reservation as a
            // forbidden SQL side effect of a later rejected save.
            var target=f.parent();var baseline=f.physical();before=f.counters();
            var normalized=WireFieldAdapter.normalize((ObjectNode)JSON.readTree("{\"legacy/phone~\":null}"),metadata);
            f.mapped++;target.updateContactPhone(null);f.saves++;
            var error=assertThrows(CheckException.class,()->target.auditAs("Check an actually submitted JSON alias").save(f.context));
            WireFieldAdapter.retainSubmittedPaths(error.getViolates(),normalized);f.zero(f.delta(before),true);assertEquals(baseline,f.physical());
            var actual=error.getViolates().stream().filter(v->v.toWire(profile).ruleId().equals("required")
                    &&v.getLocation().modelPath().equals("contact_phone")).findFirst().orElseThrow();
            assertEquals("/legacy~1phone~0",actual.getSourceInstancePath());var wires=new LinkedHashMap<String,Object>();
            for(var output:JsonFieldNamingProfile.values())wires.put(output.modelValue(),actual.toWire(output));
            f.emit("accepted-input-alias",Map.of("wires",wires,"zeroProviderIO",true,"idReservedBeforeBoundary",true),name);
            for(String scenario:List.of("unknown","equal-forward","equal-reverse","unequal-forward","unequal-reverse")) {
                String text;
                if(scenario.equals("unknown"))text="{\"unknown/field~\":{\"not_a_phone\":42}}";
                else {String first=scenario.endsWith("reverse")?"legacy/phone~":standard,last=scenario.endsWith("reverse")?standard:"legacy/phone~";
                    String value=scenario.startsWith("unequal")?"5550101":"5550100";
                    text="{"+JSON.writeValueAsString(first)+":\"5550100\","+JSON.writeValueAsString(last)+":"+JSON.writeValueAsString(value)+"}";}
                var submitted=(ObjectNode)JSON.readTree(text);baseline=f.physical();before=f.counters();
                var wire=assertThrows(TfpEndpointException.class,()->{
                    var input=WireFieldAdapter.normalize(submitted,metadata);f.mapped++;
                    target.updateContactPhone(input.values().get("contact_phone").asText());f.saves++;
                    target.auditAs("This invalid wire input must never reach Save").save(f.context);
                });
                assertEquals(scenario.equals("unknown")?"WIRE_UNKNOWN_FIELD":"WIRE_FIELD_COLLISION",wire.getCode());
                effects=f.delta(before);f.zero(effects,false);assertEquals(Integer.valueOf(0),effects.get("mapped"));assertEquals(Integer.valueOf(0),effects.get("saves"));
                assertEquals(baseline,f.physical());f.emit("input-"+scenario,Map.of("code",wire.getCode(),"mapperCalls",0,"saveCalls",0,"checkerCalls",0,"zeroProviderIO",true),name);
            }
            var input=WireFieldAdapter.normalize((ObjectNode)JSON.readTree("{"+JSON.writeValueAsString(standard)+":\"55509"+f.round+profile.ordinal()+"\"}"),metadata);
            target.updateContactPhone(input.values().get("contact_phone").asText());before=f.counters();
            target.auditAs("Persist a valid normalized JSON control").save(f.context);effects=f.delta(before);
            assertEquals(Integer.valueOf(1),effects.get("parentInserts"));assertEquals(Integer.valueOf(1),effects.get("audits"));
            f.emit("wire-positive-control",Map.of("id",target.getId(),"parentInserts",1),name);
        }
        assertEquals(26,f.observations);
        System.out.println("JAVA_CHECKER_BOUNDARY_RESULT="+JSON.writeValueAsString(Map.of("language","java","passed",true,
                "round",f.round,"observations",f.observations,"sameDatabase",true,"physical",f.physical(),
                "realJsonInputAdapter","existing-public-object-local-composition","recursiveHttpOrTfpAccepted",false,
                "fullCheckerContractAccepted",false,"allP0P1Complete",false)));
    }
}
