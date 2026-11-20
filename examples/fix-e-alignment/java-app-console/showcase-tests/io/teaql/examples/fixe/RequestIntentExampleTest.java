package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.GeneratedRuntimeModule;
import com.teaql.checkerfixadvanced.Q;
import io.teaql.core.*;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.runtime.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Generated Q/E and audited-save admission with actual SQLite, in both log modes. */
public class RequestIntentExampleTest {
    record Effects(int sqlQueries, int sqlWrites, int transactions, int commits,
                   int providerQueries, int providerMutations, int queryPolicies,
                   int mutationPolicies, int sqlLogs, int audits) {}
    static final Effects ZERO = new Effects(0,0,0,0,0,0,0,0,0,0);
    static final class Fixture {
        final SQLiteDataSource source = new SQLiteDataSource();
        final CheckerBoundaryExampleTest.Observed sql;
        final TeaQLRuntime runtime;
        final UserContext context;
        final String database;
        boolean armed;
        int providerQueries, providerMutations, queryPolicies, mutationPolicies, sqlLogs, audits;
        Fixture(boolean logging) {
            database = Objects.requireNonNull(System.getenv("TEAQL_FIX_E_DB")) + ".request-admission-" + (logging ? "on" : "off") + ".sqlite";
            source.setUrl("jdbc:sqlite:" + database); sql = new CheckerBoundaryExampleTest.Observed(source);
            var ids = new IdSpaceIdGenerator(new GeneratedFixEExampleTest.DatabaseBridge(sql)); ids.ensureIdSpaceTable();
            var service = new SqliteDataServiceExecutor("sqlite", sql, source) {
                @Override public QueryResult query(UserContext caller, QueryRequest request) {
                    if (armed) providerQueries++; return super.query(caller, request);
                }
                @Override public MutationResult mutate(UserContext caller, PersistenceMutation request) {
                    if (armed) providerMutations++; return super.mutate(caller, request);
                }
            };
            var clock = new BusinessClock() {
                public LocalDate businessDate(UserContext caller) { return LocalDate.of(2032,2,29); }
                public LocalDateTime businessDateTime(UserContext caller) { return businessDate(caller).atTime(10,15,30); }
            };
            runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default",service).dataService("sqlite",service).idGenerationService(ids)
                    .businessClock(clock).executionLogging(logging)
                    .queryPolicy(new QueryPolicy() {
                        @Override public void enforceSelect(UserContext caller, SearchRequest<?> request) { if (armed) queryPolicies++; }
                    }).mutationPolicyRegistry(key -> { if (armed) mutationPolicies++; return Optional.empty(); })
                    .logSink(new RuntimeLogSink() {
                        public void writeExecutionLog(UserContext caller, ExecutionMetadata event) { if (armed) sqlLogs++; }
                        public void writeAuditEvent(UserContext caller, RawAuditEvent event) { if (armed) audits++; }
                    }).build().install(GeneratedRuntimeModule.module());
            context = new GeneratedFixEExampleTest.Context(runtime); context.ensureSchema();
        }
        void reset() {
            sql.queries=0; sql.writes=0; sql.transactions=0; sql.commits=0;
            providerQueries=0; providerMutations=0; queryPolicies=0; mutationPolicies=0; sqlLogs=0; audits=0;
        }
        Effects effects() { return new Effects(sql.queries,sql.writes,sql.transactions,sql.commits,providerQueries,providerMutations,queryPolicies,mutationPolicies,sqlLogs,audits); }
        // The independent oracle has a read-only SQLite connection and cannot
        // create runtime evidence or increment the observed application adapter.
        Map<String,List<Map<String,Object>>> physical() throws Exception {
            var readOnly = new SQLiteDataSource(); readOnly.setUrl("jdbc:sqlite:file:" + database + "?mode=ro");
            var result = new TreeMap<String,List<Map<String,Object>>>();
            try (var connection=readOnly.getConnection()) {
                for (String table : List.of("purchase_order_data","order_item_data","teaql_id_space")) {
                    var rows = new ArrayList<Map<String,Object>>();
                    try (var statement=connection.createStatement();var data=statement.executeQuery("SELECT * FROM " + table + " ORDER BY " + (table.equals("teaql_id_space") ? "type_name" : "id"))) {
                        var meta=data.getMetaData();while(data.next()) {var row=new TreeMap<String,Object>();for(int n=1;n<=meta.getColumnCount();n++)row.put(meta.getColumnName(n),data.getObject(n));rows.add(row);}
                    }
                    result.put(table,rows);
                }
            }
            return result;
        }
    }
    static final ObjectMapper JSON = new ObjectMapper();
    static void emit(String key, Object value) throws Exception { System.out.println(key + "=" + JSON.writeValueAsString(value)); }

    @Test public void invalidRootIntentRejectsAnnotatedGeneratedGraphBeforePolicyProviderAndSql() throws Exception {
        int round=Integer.parseInt(Objects.requireNonNull(System.getenv("TEAQL_FIX_E_ROUND")));assertTrue(round==1||round==2);
        String[] blanks={null,""," \t\r\n","\u0085","\u00a0","\u2003"};
        String[] kinds={"missing","empty","ascii-whitespace","NEL","NBSP","em-space"};
        int cases=0;
        for(boolean logging:new boolean[]{false,true}) {
            var f=new Fixture(logging);
            var parent=Q.purchaseOrders().comment("Prepare the request admission parent").purpose("Use a real generated mutation graph").newEntity(f.context);
            parent.updateContactPhone("5550801"); parent.updateInternalNote(null);
            var child=Q.orderItems().comment("Prepare the annotated child").purpose("Child intent must not supply missing root intent").newEntity(f.context);
            child.updateProductName("admission child " + round); child.updateUnitPrice(BigDecimal.ONE);
            child.setComment("annotated child SECRET-CANARY"); parent.addOrderItem(child);
            long parentID=parent.getId(),childID=child.getId();assertTrue(parentID>0&&childID>0);
            var baseline=f.physical(); f.armed=true;
            for(String action:List.of("query-comment","query-purpose","mutation-comment"))for(int index=0;index<blanks.length;index++) {
                String blank=blanks[index];f.reset();
                if(action.equals("mutation-comment"))parent.setComment(blank);
                RequestIntentException error;
                try {
                    if(action.equals("mutation-comment"))f.runtime.saveGraph(f.context,parent);
                    else Q.purchaseOrders().selectOrderItemListWith(Q.orderItems().limit(1).comment("annotated child query SECRET-CANARY"))
                            .limit(1).comment(action.equals("query-comment")?blank:"load SECRET-CANARY")
                            .purpose(action.equals("query-purpose")?blank:"render SECRET-CANARY").executeForList(f.context);
                    throw new AssertionError("Missing request intent was accepted");
                }catch(RequestIntentException rejected){error=rejected;}
                String code=action.equals("query-purpose")?"QUERY_PURPOSE_REQUIRED":"REQUEST_COMMENT_REQUIRED";
                String field=action.equals("query-purpose")?"purpose":"comment",requestKind=action.equals("mutation-comment")?"mutation":"query";
                assertEquals(code,error.getCode());assertEquals(field,error.getField());assertEquals(requestKind,error.getRequestKind());
                assertFalse(error.getMessage().contains("SECRET-CANARY"));assertEquals(ZERO,f.effects());assertEquals(baseline,f.physical());
                assertEquals(Long.valueOf(parentID),parent.getId());assertEquals(Long.valueOf(childID),child.getId());
                assertNull(parent.getVersion());assertNull(child.getVersion());
                assertEquals("annotated child SECRET-CANARY",child.getComment());assertTrue(f.context.getTraceChain().isEmpty());
                emit("JAVA_REQUEST_ADMISSION_CASE",Map.ofEntries(Map.entry("round",round),Map.entry("logging",logging),Map.entry("action",action),Map.entry("inputKind",kinds[index]),
                        Map.entry("code",code),Map.entry("field",field),Map.entry("requestKind",requestKind),Map.entry("effects",f.effects()),Map.entry("parentID",parentID),Map.entry("childID",childID),
                        Map.entry("annotatedChildRetained",true),Map.entry("physicalAndIdSpaceUnchanged",true),Map.entry("identitiesAndVersionsRetained",true),Map.entry("privateValuesNotEchoed",true),Map.entry("passed",true)));
                cases++;
            }
            f.reset();var saved=parent.auditAs("Calibrate request admission with one valid generated save").save(f.context);
            var loaded=Q.purchaseOrders().withIdIs(saved.getId()).selectOrderItemListWith(Q.orderItems().limit(1))
                    .limit(1).comment("Read the accepted graph").purpose("Calibrate every observer after rejected requests").executeForOne(f.context);
            var beforeE=f.effects();assertEquals("5550801",E.purchaseOrder(loaded).getContactPhone().eval());
            assertEquals(Integer.valueOf(1),E.purchaseOrder(loaded).getOrderItemList().size().eval());
            assertEquals("admission child " + round,E.purchaseOrder(loaded).getOrderItemList().first().getProductName().eval());assertEquals(beforeE,f.effects());
            var effects=f.effects();assertTrue(effects.sqlQueries()>0&&effects.sqlWrites()>0&&effects.transactions()>0&&effects.commits()>0);
            assertTrue(effects.providerQueries()>0&&effects.providerMutations()>0&&effects.queryPolicies()>0&&effects.mutationPolicies()>0&&effects.audits()>0);
            if(logging)assertTrue(effects.sqlLogs()>0);else assertEquals(0,effects.sqlLogs());
            var stored=f.physical();assertEquals(round,stored.get("purchase_order_data").size());assertEquals(round,stored.get("order_item_data").size());
            emit("JAVA_REQUEST_ADMISSION_CONTROL",Map.ofEntries(Map.entry("round",round),Map.entry("logging",logging),Map.entry("parentID",parentID),Map.entry("childID",childID),
                    Map.entry("effects",effects),Map.entry("parents",round),Map.entry("children",round),Map.entry("eProviderReads",0),Map.entry("passed",true)));
        }
        assertEquals(36,cases);emit("JAVA_REQUEST_ADMISSION_RESULT",Map.of("round",round,"cases",cases,"controls",2,"passed",true));
    }
}
