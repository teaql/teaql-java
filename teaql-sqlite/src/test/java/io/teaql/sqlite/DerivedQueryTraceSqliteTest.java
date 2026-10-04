package io.teaql.sqlite;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.*;
import io.teaql.core.sql.*;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** #202: native dynamic aggregation -> actual SQLite -> safe statement-owned provenance. */
public class DerivedQueryTraceSqliteTest {
    private static final String PRIVATE_NAME = "PRIVATE-DOCUMENT-NAME";

    public abstract static class Row extends BaseEntity {
        private final Map<String,Object> values = new HashMap<>();
        @Override public Object __internalGet(String field) {
            return field.equals("name") || field.equals("state") || field.equals("document") || field.equals("lines") || field.equals("documentNumber")
                    ? values.get(field) : super.__internalGet(field);
        }
        @Override public void __internalSet(String field, Object value) {
            if (field.equals("name") || field.equals("state") || field.equals("document") || field.equals("lines") || field.equals("documentNumber"))
                values.put(field,value);
            else super.__internalSet(field,value);
        }
    }
    public static final class TraceDocument extends Row {
        @Override public String typeName() { return "TraceDocument"; }
    }
    public static final class TraceLine extends Row {
        @Override public String typeName() { return "TraceLine"; }
    }
    private static final class Request<T extends Entity> extends BaseRequest<T> {
        private final String type;
        Request(Class<T> type, Supplier<T> factory) {
            super(type,factory); this.type=type.getSimpleName();
        }
        @Override public String getTypeName() { return type; }
        Request<T> where(String field, Operator operator, Object value) {
            appendSearchCriteria(createBasicSearchCriteria(field,operator,value)); return this;
        }
        Request<T> intent(String comment, String purpose) {
            internalComment(comment); internalPurpose(purpose); return this;
        }
    }

    private static final class Fixture {
        final List<ExecutionMetadata> sql = new CopyOnWriteArrayList<>();
        final List<DefaultQueryResult> returnedQueries = new CopyOnWriteArrayList<>();
        final SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        final DefaultUserContext context;
        final SqliteDataServiceExecutor provider;
        final TraceDocument document;
        final TraceLine open;
        Fixture(boolean logging) throws Exception {
            this(logging, JdbcSqlExecutor::new);
        }
        Fixture(boolean logging, Function<SQLiteDataSource, JdbcSqlExecutor> driverFactory) throws Exception {
            var source = new SQLiteDataSource();
            source.setUrl("jdbc:sqlite:" + Files.createTempFile("teaql-derived-trace-", ".db"));
            var documents = descriptor(TraceDocument.class,TraceDocument::new);
            var lines = descriptor(TraceLine.class,TraceLine::new);
            var state = (GenericSQLProperty) lines.addSimpleProperty("state",String.class);
            state.setColumnType("VARCHAR(255)");
            var documentNumber = (GenericSQLProperty) lines.addSimpleProperty("documentNumber",Long.class);
            documentNumber.setColumnType("BIGINT");
            var relation = (GenericSQLRelation) lines.addObjectProperty(metadata,"document", "TraceDocument", "lines",TraceDocument.class);
            relation.setColumnType("BIGINT");
            var driver = driverFactory.apply(source);
            provider = new SqliteDataServiceExecutor("sqlite",driver,source) {
                @Override public QueryResult query(UserContext caller, QueryRequest request) {
                    var result = super.query(caller, request);
                    returnedQueries.add((DefaultQueryResult) result);
                    return result;
                }
            };
            var runtime = TeaQLRuntime.builder().metadata(metadata).dataService("sqlite",provider)
                    .queryExecutionLogging(logging).logSink((caller,entry)->sql.add(entry)).build();
            context = new DefaultUserContext(runtime); context.ensureSchema();
            document = create(new TraceDocument(),100,PRIVATE_NAME);
            document.auditAs("seed the selected document").save(context);
            var unrelated = create(new TraceDocument(),200,"Other document");
            unrelated.auditAs("seed an unrelated document").save(context);
            open = create(new TraceLine(),101,"Selected open line");
            seedLine(open,document,"OPEN");
            seedLine(create(new TraceLine(),102,"Selected closed line"),document,"CLOSED");
            seedLine(create(new TraceLine(),201,"Other open line"),unrelated,"OPEN");
            sql.clear();
        }
        private <T extends Row> SQLEntityDescriptor descriptor(Class<T> type,Supplier<T> supplier) {
            var descriptor = new SQLEntityDescriptor();
            descriptor.setType(type.getSimpleName()); descriptor.setTargetType(type);
            descriptor.setEntitySupplier(supplier); descriptor.setDataService("sqlite");
            descriptor.setAuditMaskFields(List.of("name"));
            for (String field : List.of("id","version","name")) {
                var property = (GenericSQLProperty) descriptor.addSimpleProperty(field,field.equals("name")?String.class:Long.class);
                property.setColumnType(field.equals("name")?"VARCHAR(255)":"BIGINT");
            }
            metadata.register(descriptor); return descriptor;
        }
        private <T extends Row> T create(T row,long id,String name) {
            row.__internalInitializeNewEntityId(id); row.updateProperty("name",name); return row;
        }
        private void seedLine(TraceLine line,TraceDocument owner,String state) {
            line.updateProperty("document",owner); line.updateProperty("state",state);
            line.updateProperty("documentNumber",owner.getId());
            line.auditAs("seed a document line").save(context);
        }
        Request<TraceDocument> documents() {
            var request = new Request<>(TraceDocument.class,TraceDocument::new);
            request.bindMetadata(metadata);
            for (String field : List.of("id","version","name")) request.selectProperty(field);
            return request;
        }
        Request<TraceLine> lines() {
            var request = new Request<>(TraceLine.class,TraceLine::new);
            request.bindMetadata(metadata);
            for (String field : List.of("id","version","name","document","documentNumber","state")) request.selectProperty(field);
            return request;
        }
        Request<TraceDocument> withOpenCount() {
            var count = lines().where("state",Operator.EQUAL,"OPEN");
            count.setPartitionProperty("document"); count.count("count");
            var root = documents(); root.addSingleAggregateDynamicProperty("openLineCount",count); return root;
        }
    }

    /** Hold completed physical root reads, not merely two starts at a barrier. */
    private static final class PausedRoots extends JdbcSqlExecutor {
        final CountDownLatch bothReturned = new CountDownLatch(2);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger roots = new AtomicInteger();
        volatile boolean armed;
        PausedRoots(SQLiteDataSource source) { super(source); }
        private void hold(String sql) {
            if (!armed || !sql.toLowerCase(Locale.ROOT).contains("trace_document_data")) return;
            roots.incrementAndGet(); bothReturned.countDown();
            try {
                assertTrue("completed SQLite roots must be released", release.await(10, TimeUnit.SECONDS));
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt(); throw new AssertionError(failure);
            }
        }
        @Override public <T extends Entity> List<T> query(String sql, Object[] args, CompiledRowMapper<T> mapper) {
            var rows = super.query(sql, args, mapper); hold(sql); return rows;
        }
        @Override public List<Map<String, Object>> queryForList(String sql, Object[] args) {
            var rows = super.queryForList(sql, args); hold(sql); return rows;
        }
    }

    @Test public void twoLiveSqliteQueriesOnOneContextKeepPhysicalIntentAndRelationsIsolated() throws Exception {
        for (boolean logging : List.of(false, true)) {
            var driver = new AtomicReference<PausedRoots>();
            var fixture = new Fixture(logging, source -> {
                var value = new PausedRoots(source); driver.set(value); return value;
            });
            var gate = driver.get(); gate.armed = true;
            fixture.context.pushTrace("unrelated application diagnostic");
            var baseline = fixture.context.getTraceChain();
            var workers = Executors.newFixedThreadPool(2);
            try {
                var alpha = fixture.documents().where("id", Operator.EQUAL, 100L)
                        .intent("load alpha graph", "render alpha graph");
                var beta = fixture.documents().where("id", Operator.EQUAL, 200L)
                        .intent("load beta graph", "render beta graph");
                alpha.setSize(1); beta.setSize(1);
                var alphaLines = fixture.lines(); alphaLines.setSize(10);
                var betaLines = fixture.lines(); betaLines.setSize(10);
                alpha.enhanceRelation("lines", alphaLines); beta.enhanceRelation("lines", betaLines);
                var first = workers.submit(() -> fixture.context.getRuntime().executeForList(fixture.context, alpha));
                var second = workers.submit(() -> fixture.context.getRuntime().executeForList(fixture.context, beta));
                assertTrue("two physical roots must return before either query completes",
                        gate.bothReturned.await(10, TimeUnit.SECONDS));
                assertFalse(first.isDone()); assertFalse(second.isDone());
                assertEquals(2, gate.roots.get()); assertTrue(fixture.sql.isEmpty());
                assertTrue(fixture.returnedQueries.isEmpty());
                assertEquals("live requests must not put their frames on Context", baseline, fixture.context.getTraceChain());
                System.out.printf("LIVE_SQLITE_BARRIER logging=%s physicalRoots=2 unfinishedQueries=2 contextUnchanged=true logs=0%n", logging);
                gate.release.countDown();
                var results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
                assertEquals("two roots and two real derived provider requests", 4, fixture.returnedQueries.size());
                for (int i = 0; i < results.size(); i++) {
                    var rows = results.get(i); var label = i == 0 ? "alpha" : "beta";
                    assertEquals(1, rows.size());
                    assertEquals(Long.valueOf(i == 0 ? 100 : 200), rows.get(0).getId());
                    SmartList<?> children = rows.get(0).getProperty("lines");
                    assertEquals(i == 0 ? 2 : 1, children.size());
                    assertEquals(i == 0 ? List.of(101L, 102L) : List.of(201L),
                            children.stream().map(Entity::getId).sorted().toList());
                    var ownResults = fixture.returnedQueries.stream()
                            .filter(r -> r.getResult().get(0).typeName().equals("TraceDocument"))
                            .filter(r -> r.statements().get(0).getComment().equals("load " + label + " graph")).toList();
                    assertEquals(1, ownResults.size());
                    var result = ownResults.get(0); assertEquals(2, result.statements().size());
                    for (int depth = 0; depth < 2; depth++) {
                        var statement = result.statements().get(depth);
                        assertEquals("load " + label + " graph", statement.getComment());
                        assertEquals("render " + label + " graph", statement.getPurpose());
                        assertPath(statement, "TraceDocument", depth == 0 ? List.of() : List.of("lines"));
                        assertEquals(depth == 0 ? List.of() : List.of("TraceDocument.lines"),
                                statement.getTraceChain().stream().filter(n -> n.getKind() == TraceKind.RELATION)
                                        .map(TraceNode::getComment).toList());
                        assertEquals("query", statement.getTraceChain().get(0).getComment());
                        assertEquals("", statement.getTraceChain().get(1).getComment());
                    }
                }
                assertEquals(logging ? 4 : 0, fixture.sql.size());
                if (logging) {
                    for (String label : List.of("alpha", "beta")) {
                        var own = fixture.sql.stream().filter(e -> e.getComment().equals("load " + label + " graph")).toList();
                        assertEquals(2, own.size());
                        assertTrue(own.stream().allMatch(e -> e.getPurpose().equals("render " + label + " graph")));
                        assertPath(own.get(0), "TraceDocument", List.of());
                        assertPath(own.get(1), "TraceDocument", List.of("lines"));
                    }
                }
                assertEquals(baseline, fixture.context.getTraceChain());
                System.out.printf("LIVE_SQLITE_RESULT logging=%s physicalStatements=4 inheritedRelations=2 safeLogs=%d contextUnchanged=true%n", logging, fixture.sql.size());
            } finally {
                gate.release.countDown(); workers.shutdownNow();
                assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
            }
        }
    }

    private static void assertPath(ExecutionMetadata entry,String root,List<String> relations) {
        var kinds = new ArrayList<>(List.of(TraceKind.OPERATION,TraceKind.REQUEST));
        relations.forEach(relation -> kinds.add(TraceKind.RELATION));
        kinds.add(TraceKind.PROVIDER); kinds.add(TraceKind.SQL);
        assertEquals(kinds,entry.getTraceChain().stream().map(TraceNode::getKind).toList());
        assertEquals(root,entry.getTraceChain().get(0).getName());
        assertEquals(root,entry.getTraceChain().get(1).getName());
        assertEquals("sqlite",entry.getTraceChain().get(entry.getTraceChain().size()-2).getName());
        assertEquals("select",entry.getTraceChain().get(entry.getTraceChain().size()-1).getName());
        assertEquals(relations,entry.getTraceChain().stream().filter(node->node.getKind()==TraceKind.RELATION)
                .map(TraceNode::getName).toList());
    }

    @Test public void childMembershipSurvivesFilteredForwardIdentityReferences() throws Exception {
        verifyFilteredReferenceMembership(false);
    }

    @Test public void nestedChildMembershipSurvivesFilteredForwardIdentityReferences() throws Exception {
        verifyFilteredReferenceMembership(true);
    }

    private void verifyFilteredReferenceMembership(boolean nested) throws Exception {
        for (boolean logging : List.of(false, true)) {
            for (boolean filtered : List.of(false, true)) {
                for (int threshold : List.of(0, 32)) {
                    verifyFilteredReferenceMembership(nested, logging, filtered, threshold);
                }
            }
        }
    }

    private void verifyFilteredReferenceMembership(boolean nested, boolean logging, boolean filtered, int threshold) throws Exception {
                var fixture = new Fixture(logging);
                var selectedParent = fixture.documents().where("name", Operator.EQUAL,
                        filtered ? "ABSENT-DOCUMENT" : PRIVATE_NAME);
                var children = fixture.lines();
                children.setSize(10);
                children.topNProbeParentThreshold(threshold);
                children.enhanceRelation("document", selectedParent);
                var documents = fixture.withOpenCount().where("id", Operator.EQUAL, 100L);
                documents.setSize(1);
                documents.enhanceRelation("lines", children);
                Request<?> request = documents;
                if (nested) {
                    var outer = fixture.lines().where("id", Operator.EQUAL, 101L);
                    outer.setSize(1);
                    outer.enhanceRelation("document", documents);
                    request = outer;
                }
                request.intent("load selected document graph", "verify filtered membership and safe E access");
                var result = (DefaultQueryResult) fixture.provider.query(fixture.context, new DefaultQueryRequest(request));
                assertSame("internal projection must preserve the caller's nested load",
                        selectedParent, children.enhanceRelations().get("document"));
                assertEquals(1, result.getResult().size());
                TraceDocument owner = nested
                        ? (TraceDocument) result.getResult().get(0).getProperty("document")
                        : (TraceDocument) result.getResult().get(0);
                SmartList<?> loaded = owner.getProperty("lines");
                assertNotNull(loaded);
                assertEquals(2, loaded.size());
                assertEquals(1, ((Number) owner.getDynamicProperty("openLineCount")).intValue());
                for (Entity child : loaded) {
                    var reference = (TraceDocument) child.getProperty("document");
                    assertNotNull("Java retains the FK identity stub, not null", reference);
                    assertEquals(Long.valueOf(100), reference.getId());
                    assertEquals(!filtered, reference.isPropertyLoaded("name"));
                    var expression = new io.teaql.core.value.BaseEntityExpression<TraceDocument, TraceDocument>() {
                        @Override public TraceDocument eval(TraceDocument value) { return value; }
                        @Override public TraceDocument $getRoot() { return reference; }
                    };
                    if (filtered) {
                        assertThrows(io.teaql.core.value.TeaQLNotLoadedException.class,
                                () -> expression.loaded("name", e -> e.getProperty("name")).eval());
                    } else {
                        assertEquals(PRIVATE_NAME, expression.loaded("name", e -> e.getProperty("name")).eval());
                    }
                    assertTrue(child.getUpdatedProperties().isEmpty());
                }
                int expected = nested ? 5 : 4;
                assertEquals(expected, result.statements().size());
                assertEquals(threshold == 0, result.statements().get(nested ? 2 : 1)
                        .getParameterizedQuery().toUpperCase(Locale.ROOT).contains("ROW_NUMBER"));
                assertEquals(logging ? expected : 0, fixture.sql.size());
                var relations = nested ? List.of("document", "lines", "document") : List.of("lines", "document");
                assertPath(result.statements().get(expected - 2), nested ? "TraceLine" : "TraceDocument", relations);
                assertPath(result.statements().get(expected - 1), nested ? "TraceLine" : "TraceDocument",
                        nested ? List.of("document", "lines") : List.of("lines"));
                assertTrue(fixture.context.getTraceChain().isEmpty());
                var independent = (DefaultQueryResult) fixture.provider.query(fixture.context, new DefaultQueryRequest(
                        fixture.lines().where("id", Operator.EQUAL, 101L).intent("independent line", "verify original FK")));
                assertEquals(Long.valueOf(100), ((TraceDocument) independent.getResult().get(0).getProperty("document")).getId());
                assertEquals(1, independent.statements().size());
                assertPath(independent.statements().get(0), "TraceLine", List.of());
                System.out.printf("PASS Java relation membership: nested=%s logging=%s filtered=%s threshold=%s; identity stub retained, E guarded%n",
                        nested, logging, filtered, threshold);
    }

    @Test public void dynamicCountRetainsItsRootRelationAndPrivateIntentProvenance() throws Exception {
        var fixture = new Fixture(true);
        var request = fixture.withOpenCount().where("name",Operator.EQUAL,PRIVATE_NAME)
                .intent("inspect " + PRIVATE_NAME,"render document statistics");
        var rows = fixture.context.getRuntime().executeForList(fixture.context,request);
        assertEquals(1,rows.size()); assertEquals(Long.valueOf(100),rows.get(0).getId());
        assertEquals(PRIVATE_NAME,rows.get(0).getProperty("name"));
        assertEquals(1,((Number) rows.get(0).getDynamicProperty("openLineCount")).intValue());
        assertEquals(2,fixture.sql.size());
        assertPath(fixture.sql.get(0),"TraceDocument",List.of());
        assertPath(fixture.sql.get(1),"TraceDocument",List.of("lines"));
        for (var entry : fixture.sql) {
            assertFalse(entry.getComment().contains(PRIVATE_NAME));
            assertTrue(entry.getComment().contains("[REDACTED]"));
            assertEquals("render document statistics",entry.getPurpose());
        }
        assertEquals("inspect " + PRIVATE_NAME,request.comment());
        assertTrue(fixture.context.getTraceChain().isEmpty());
    }

    @Test public void dynamicCountInsideALoadedRelationPreservesAllAncestorFrames() throws Exception {
        var fixture = new Fixture(true);
        var request = fixture.lines().where("id",Operator.EQUAL,fixture.open.getId())
                .intent("inspect the selected line context","render parent statistics");
        request.selectProperty("id"); request.selectProperty("version"); request.selectProperty("document");
        request.enhanceRelation("document",fixture.withOpenCount());
        var rows = fixture.context.getRuntime().executeForList(fixture.context,request);
        assertEquals(1,rows.size());
        var parent = (TraceDocument) rows.get(0).getProperty("document");
        assertEquals(Long.valueOf(100),parent.getId());
        assertEquals(1,((Number) parent.getDynamicProperty("openLineCount")).intValue());
        assertEquals(3,fixture.sql.size());
        assertPath(fixture.sql.get(0),"TraceLine",List.of());
        assertPath(fixture.sql.get(1),"TraceLine",List.of("document"));
        assertPath(fixture.sql.get(2),"TraceLine",List.of("document","lines"));
        assertTrue(fixture.sql.stream().allMatch(entry -> request.comment().equals(entry.getComment())
                && request.purpose().equals(entry.getPurpose())));
        assertTrue(fixture.context.getTraceChain().isEmpty());
    }

    @Test public void derivedCountStillExecutesWithLoggingDisabled() throws Exception {
        var fixture = new Fixture(false);
        var request = fixture.withOpenCount().where("id",Operator.EQUAL,100L)
                .intent("inspect the selected document","render counts without SQL logging");
        var rows = fixture.context.getRuntime().executeForList(fixture.context,request);
        assertEquals(1,rows.size());
        assertEquals(1,((Number) rows.get(0).getDynamicProperty("openLineCount")).intValue());
        assertTrue(fixture.sql.isEmpty()); assertTrue(fixture.context.getTraceChain().isEmpty());
    }

    @Test public void numericPartitionInheritsTheRootWithoutInventingAModelRelation() throws Exception {
        var fixture = new Fixture(true);
        var count = fixture.lines().where("state",Operator.EQUAL,"OPEN");
        count.setPartitionProperty("documentNumber"); count.count("count");
        var request = fixture.documents().where("id",Operator.EQUAL,100L)
                .intent("inspect an explicit numeric partition","render partition statistics");
        request.addSingleAggregateDynamicProperty("openLineCount",count);
        var rows = fixture.context.getRuntime().executeForList(fixture.context,request);
        assertEquals(1,rows.size()); assertEquals(Long.valueOf(100),rows.get(0).getId());
        assertEquals(1,((Number) rows.get(0).getDynamicProperty("openLineCount")).intValue());
        assertEquals(2,fixture.sql.size());
        for (var entry : fixture.sql) {
            assertPath(entry,"TraceDocument",List.of());
            assertEquals(request.comment(),entry.getComment()); assertEquals(request.purpose(),entry.getPurpose());
        }
        assertTrue(fixture.context.getTraceChain().isEmpty());
    }

    @Test public void returnedStatementsRetainNestedPathsAndPrivacyInBothLoggingModes() throws Exception {
        for (boolean logging : List.of(false, true)) {
            var fixture = new Fixture(logging);
            var request = fixture.lines().where("id", Operator.EQUAL, fixture.open.getId())
                    .intent("inspect " + PRIVATE_NAME, "render nested counts");
            request.enhanceRelation("document", fixture.withOpenCount().where("name", Operator.EQUAL, PRIVATE_NAME));
            var result = fixture.provider.query(fixture.context, new DefaultQueryRequest(request));
            assertEquals(3, result.statements().size());
            assertPath(result.statements().get(0), "TraceLine", List.of());
            assertPath(result.statements().get(1), "TraceLine", List.of("document"));
            assertPath(result.statements().get(2), "TraceLine", List.of("document", "lines"));
            assertEquals(logging ? 3 : 0, fixture.sql.size());
            for (var entry : result.statements()) {
                assertEquals("success", entry.getExecutionOutcome());
                assertEquals(request.comment(), entry.getComment());
            }
            // The parent predicate has reached the collector before its derived aggregate.
            var safe = io.teaql.runtime.LogPrivacy.sql(result.statements().get(2), false);
            assertFalse(safe.getComment().contains(PRIVATE_NAME));
            assertThrows(UnsupportedOperationException.class, () -> result.statements().clear());
            var later = fixture.provider.query(fixture.context, new DefaultQueryRequest(
                    fixture.documents().where("id", Operator.EQUAL, 200L).intent("independent", "verify request ownership")));
            assertEquals(1, later.statements().size());
            assertEquals("independent", later.statements().get(0).getComment());
            assertEquals(3, result.statements().size());
            assertTrue(fixture.context.getTraceChain().isEmpty());
        }
    }

    @Test public void returnedAggregateAndEmptyResultStillCarryPhysicalEvidence() throws Exception {
        var fixture = new Fixture(false);
        var aggregate = fixture.lines().intent("count lines", "render totals");
        aggregate.count("count");
        var count = fixture.provider.query(fixture.context, new DefaultQueryRequest(aggregate));
        assertEquals(1, count.statements().size());
        assertPath(count.statements().get(0), "TraceLine", List.of());
        var empty = fixture.provider.query(fixture.context, new DefaultQueryRequest(
                fixture.documents().where("id", Operator.EQUAL, -1L).intent("find absent row", "verify empty evidence")));
        assertEquals(1, empty.statements().size());
        assertEquals(Integer.valueOf(0), empty.statements().get(0).getResultCount());
        assertTrue(fixture.sql.isEmpty());
    }

    @Test public void returnedCursorsOwnTerminalEvidenceAcrossInterleavedConsumption() throws Exception {
        for (boolean logging : List.of(false, true)) {
            var fixture = new Fixture(logging);
            var request = fixture.documents().where("name", Operator.EQUAL, PRIVATE_NAME)
                    .intent("inspect " + PRIVATE_NAME, "retain cursor evidence");
            try (var first = fixture.provider.<TraceDocument>queryForCursor(fixture.context, new DefaultQueryRequest(request));
                 var second = fixture.provider.<TraceDocument>queryForCursor(fixture.context, new DefaultQueryRequest(
                         fixture.documents().intent("independent stream", "verify early close")))) {
                var before = first.statements();
                assertTrue(before.isEmpty()); assertTrue(second.statements().isEmpty());
                assertEquals(1, second.stream().limit(1).toList().size());
                assertTrue("short circuit is not closed yet", second.statements().isEmpty());
                second.close();
                assertEquals("cancelled", second.statements().get(0).getExecutionOutcome());
                assertEquals("independent stream", second.statements().get(0).getComment());
                assertEquals(1, first.stream().toList().size());
                assertTrue("prior snapshots stay immutable", before.isEmpty());
                assertEquals(1, first.statements().size());
                var fact = first.statements().get(0);
                assertPath(fact, "TraceDocument", List.of());
                assertEquals("success", fact.getExecutionOutcome());
                assertEquals(Integer.valueOf(1), fact.getResultCount());
                assertFalse(LogPrivacy.sql(fact, false).getComment().contains(PRIVATE_NAME));
                assertThrows(UnsupportedOperationException.class, () -> first.statements().clear());
            }
            try (var failed = fixture.provider.<TraceDocument>queryForCursor(fixture.context, new DefaultQueryRequest(
                    fixture.documents().intent("failed consumer", "retain failure outcome")))) {
                var expected = new IllegalStateException("consumer failure");
                assertSame(expected, assertThrows(IllegalStateException.class,
                        () -> failed.stream().forEach(row -> { throw expected; })));
                assertEquals("failure", failed.statements().get(0).getExecutionOutcome());
                assertEquals(Integer.valueOf(1), failed.statements().get(0).getResultCount());
            }
            assertEquals(logging ? 3 : 0, fixture.sql.size());
            assertTrue(fixture.context.getTraceChain().isEmpty());
        }
    }
}
