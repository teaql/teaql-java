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
        final SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        final DefaultUserContext context;
        final SqliteDataServiceExecutor provider;
        final TraceDocument document;
        final TraceLine open;
        Fixture(boolean logging) throws Exception {
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
            var driver = new JdbcSqlExecutor(source);
            provider = new SqliteDataServiceExecutor("sqlite",driver,source);
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
