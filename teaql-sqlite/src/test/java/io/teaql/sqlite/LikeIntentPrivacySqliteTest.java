package io.teaql.sqlite;

import io.teaql.core.*;
import io.teaql.core.criteria.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.*;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Native typed predicates -> real SQLite binds -> governed SQL diagnostics. No supplied trace frames. */
@RunWith(Parameterized.class)
public class LikeIntentPrivacySqliteTest {
    @Parameterized.Parameters(name="{0}, marked={1}, logging={2}")
    public static Collection<Object[]> cases() {
        var cases = new ArrayList<Object[]>();
        for (var op : List.of(Operator.CONTAIN, Operator.NOT_CONTAIN, Operator.BEGIN_WITH,
                Operator.NOT_BEGIN_WITH, Operator.END_WITH, Operator.NOT_END_WITH))
            for (boolean marked : List.of(false, true))
                for (boolean logging : List.of(false, true)) cases.add(new Object[]{op, marked, logging});
        return cases;
    }
    private final Operator op;
    private final boolean marked, logging;
    public LikeIntentPrivacySqliteTest(Operator op, boolean marked, boolean logging) {
        this.op = op; this.marked = marked; this.logging = logging;
    }
    private String field() { return marked ? "name" : "state"; }
    private boolean negative() { return op.name().startsWith("NOT_"); }
    private String pattern(String operand) {
        return switch (op) {
            case CONTAIN, NOT_CONTAIN -> "%" + operand + "%";
            case BEGIN_WITH, NOT_BEGIN_WITH -> operand + "%";
            default -> "%" + operand;
        };
    }
    static final class Request<T extends Entity> extends BaseRequest<T> {
        private final String type;
        Request(Class<T> type, Supplier<T> factory) { super(type, factory); this.type = type.getSimpleName(); }
        @Override public String getTypeName() { return type; }
        Request<T> where(String field, Operator op, String value) {
            appendSearchCriteria(createBasicSearchCriteria(field, op, value)); return this;
        }
        Request<T> intent(String text) { internalComment("inspect " + text); internalPurpose("render " + text); return this; }
    }
    record Bind(String sql, List<Object> values) {}
    static final class Fixture {
        final List<Bind> binds = new ArrayList<>();
        final List<ExecutionMetadata> safe = new ArrayList<>();
        final List<QueryIntent> policies = new ArrayList<>();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        final DefaultUserContext context;
        final JdbcSqlExecutor driver;
        final SqliteDataServiceExecutor provider;
        QueryResult last;
        Fixture(boolean logging) throws Exception {
            var source = new SQLiteDataSource();
            source.setUrl("jdbc:sqlite:" + Files.createTempFile("teaql-like-intent-", ".db"));
            var documents = descriptor(DerivedQueryTraceSqliteTest.TraceDocument.class, DerivedQueryTraceSqliteTest.TraceDocument::new);
            var lines = descriptor(DerivedQueryTraceSqliteTest.TraceLine.class, DerivedQueryTraceSqliteTest.TraceLine::new);
            var relation = (GenericSQLRelation) lines.addObjectProperty(metadata, "document", "TraceDocument", "lines",
                    DerivedQueryTraceSqliteTest.TraceDocument.class);
            relation.setColumnType("BIGINT");
            driver = new JdbcSqlExecutor(source) {
                @Override public List<Map<String,Object>> queryForList(String sql, Object[] args) {
                    binds.add(new Bind(sql, Arrays.asList(args.clone()))); return super.queryForList(sql,args);
                }
                @Override public <T extends Entity> List<T> query(String sql, Object[] args, CompiledRowMapper<T> mapper) {
                    binds.add(new Bind(sql, Arrays.asList(args.clone()))); return super.query(sql,args,mapper);
                }
            };
            provider = new SqliteDataServiceExecutor("sqlite", driver, source) {
                @Override public QueryResult query(UserContext context, QueryRequest request) {
                    last = super.query(context, request); return last;
                }
            };
            var text = new DefaultTextRuntimeLogSink(new PrintStream(output));
            var runtime = TeaQLRuntime.builder().metadata(metadata).dataService("sqlite", provider)
                    .queryExecutionLogging(logging).mutationExecutionLogging(false)
                    .queryPolicy(new QueryPolicy() {
                        @Override public void enforceSelect(UserContext caller, SearchRequest<?> request) {
                            policies.add(QueryIntent.of(request.comment(), request.purpose()));
                        }
                    }).logSink((caller, entry) -> { safe.add(entry); text.writeExecutionLog(caller, entry); }).build();
            context = new DefaultUserContext(runtime); context.ensureSchema();
            driver.update("INSERT INTO trace_document_data(id,version,name,state) VALUES(1,1,?,?),(2,1,?,?)",
                    new Object[]{"FIRST-SECRET", "FIRST-SECRET", "SECOND-SECRET", "SECOND-SECRET"});
            driver.update("INSERT INTO trace_line_data(id,version,name,state,document) VALUES(11,1,?,?,1),(12,1,?,?,1)",
                    new Object[]{"FIRST-SECRET", "FIRST-SECRET", "SECOND-SECRET", "SECOND-SECRET"});
            clear();
        }
        private <T extends DerivedQueryTraceSqliteTest.Row> SQLEntityDescriptor descriptor(Class<T> type, Supplier<T> supplier) {
            var d = new SQLEntityDescriptor(); d.setType(type.getSimpleName()); d.setTargetType(type);
            d.setEntitySupplier(supplier); d.setDataService("sqlite"); d.setAuditMaskFields(List.of("name"));
            for (String name : List.of("id", "version", "name", "state")) {
                boolean text = name.equals("name") || name.equals("state");
                var p = (GenericSQLProperty) d.addSimpleProperty(name, text ? String.class : Long.class);
                p.setColumnType(text ? "VARCHAR(255)" : "BIGINT");
            }
            metadata.register(d); return d;
        }
        Request<DerivedQueryTraceSqliteTest.TraceDocument> documents() {
            return request(DerivedQueryTraceSqliteTest.TraceDocument.class, DerivedQueryTraceSqliteTest.TraceDocument::new);
        }
        Request<DerivedQueryTraceSqliteTest.TraceLine> lines() {
            var request = request(DerivedQueryTraceSqliteTest.TraceLine.class, DerivedQueryTraceSqliteTest.TraceLine::new);
            request.selectProperty("document"); return request;
        }
        private <T extends Entity> Request<T> request(Class<T> type, Supplier<T> factory) {
            var request = new Request<>(type, factory); request.bindMetadata(metadata);
            for (String field : List.of("id", "version", "name", "state")) request.selectProperty(field);
            request.top(10); return request;
        }
        SmartList<?> run(Request<?> request) { return context.getRuntime().executeForList(context, request); }
        void clear() { binds.clear(); safe.clear(); policies.clear(); output.reset(); }
    }
    private void assertProjection(Fixture f, Request<?> request, String operand, boolean privateOperand, int count) {
        assertEquals(count, f.binds.size());
        assertEquals(count, f.last.statements().size());
        assertEquals(logging ? count : 0, f.safe.size());
        assertEquals("inspect " + operand, request.comment());
        assertEquals("render " + operand, request.purpose());
        assertEquals(request.comment(), f.policies.get(0).comment());
        assertEquals(request.purpose(), f.policies.get(0).purpose());
        for (var raw : f.last.statements()) {
            assertEquals(request.comment(), raw.getComment());
            assertEquals(request.purpose(), raw.getPurpose());
            var projected = LogPrivacy.sql(raw, false);
            String expected = privateOperand ? "[REDACTED]" : operand;
            assertEquals("inspect " + expected, projected.getComment());
            assertEquals("render " + expected, projected.getPurpose());
        }
        for (var entry : f.safe) {
            String expected = privateOperand ? "[REDACTED]" : operand;
            assertEquals("inspect " + expected, entry.getComment());
            assertEquals("render " + expected, entry.getPurpose());
            assertNull(entry.getIntentRedactions());
        }
        if (logging && privateOperand) assertFalse(f.output.toString().contains(operand));
        if (!logging) assertEquals("", f.output.toString());
        assertTrue(f.context.getTraceChain().isEmpty());
    }
    @Test public void typedLikeAndCachedRebindingKeepExactOperandsPrivate() throws Exception {
        var f = new Fixture(logging);
        for (String operand : List.of("FIRST-SECRET", "SECOND-SECRET")) {
            f.clear();
            var request = f.documents().where(field(), op, operand).intent(operand);
            var rows = f.run(request);
            assertEquals(1, rows.size());
            assertEquals(negative() ? (operand.equals("FIRST-SECRET") ? 2L : 1L)
                    : (operand.equals("FIRST-SECRET") ? 1L : 2L), rows.get(0).getId().longValue());
            assertTrue(f.binds.get(0).values().contains(pattern(operand)));
            assertProjection(f, request, operand, marked, 1);
        }
        f.clear();
        var independent = f.documents().intent("FIRST-SECRET SECOND-SECRET");
        assertEquals(2, f.run(independent).size());
        assertProjection(f, independent, "FIRST-SECRET SECOND-SECRET", false, 1);
    }
    @Test public void futureChildOperandIsPrivateBeforeFirstRootStatement() throws Exception {
        var f = new Fixture(logging);
        var child = f.lines().where(field(), op, "FIRST-SECRET");
        child.topNProbeParentThreshold(0);
        var root = f.documents().intent("FIRST-SECRET");
        root.enhanceRelation("lines", child);
        var rows = f.run(root);
        assertEquals(2, rows.size());
        assertEquals(1, ((SmartList<?>) rows.get(0).getProperty("lines")).size());
        assertFalse(f.binds.get(0).values().contains(pattern("FIRST-SECRET")));
        assertTrue(f.binds.get(1).values().contains(pattern("FIRST-SECRET")));
        assertEquals(List.of(), f.last.statements().get(0).getTraceChain().stream()
                .filter(n -> n.getKind() == TraceKind.RELATION).map(TraceNode::getName).toList());
        assertEquals(List.of("lines"), f.last.statements().get(1).getTraceChain().stream()
                .filter(n -> n.getKind() == TraceKind.RELATION).map(TraceNode::getName).toList());
        assertProjection(f, root, "FIRST-SECRET", marked, 2);
        f.clear();
        var independent = f.documents().intent("FIRST-SECRET");
        assertEquals(2, f.run(independent).size());
        assertProjection(f, independent, "FIRST-SECRET", false, 1);
    }
    @Test public void literalWildcardsAndRewrittenAstDoNotInventOperands() throws Exception {
        var f = new Fixture(logging);
        for (String literal : List.of("FIRST%SECRET", "FIRST_SECRET")) {
            f.clear();
            var typed = f.documents().where(field(), op, literal).intent(literal);
            assertEquals(1, f.run(typed).size());
            assertTrue(f.binds.get(0).values().contains(pattern(literal)));
            assertProjection(f, typed, literal, marked, 1);
        }
        for (boolean rawLike : List.of(false, true)) {
            f.clear();
            var request = f.documents().intent("FIRST-SECRET");
            // A raw LIKE uses an undecorated Parameter; changing a typed AST's
            // parameter operator to EQUAL is observable and must not infer a secret.
            var parameter = new Parameter(field(), "FIRST-SECRET%", op);
            parameter.setOperator(Operator.EQUAL);
            request.appendSearchCriteria(new TwoOperatorCriteria(rawLike ? op : Operator.EQUAL,
                    new PropertyReference(field()), parameter));
            assertEquals(rawLike ? 1 : 0, f.run(request).size());
            assertTrue(f.binds.get(0).values().contains("FIRST-SECRET%"));
            assertProjection(f, request, "FIRST-SECRET", false, 1);
        }
        f.clear();
        var rewritten = f.documents().intent("FIRST-SECRET");
        var parameter = new Parameter(field(), "FIRST-SECRET", op);
        rewritten.appendSearchCriteria(new TwoOperatorCriteria(Operator.EQUAL,
                new PropertyReference(field()), parameter));
        assertEquals(0, f.run(rewritten).size());
        assertTrue(f.binds.get(0).values().contains(pattern("FIRST-SECRET")));
        assertProjection(f, rewritten, "FIRST-SECRET", false, 1);
        assertEquals("FIRST-SECRET", parameter.getValue());
        assertEquals(op, parameter.getOperator());
    }

    @Test public void futureEqualityAndSetOperandsArePrivateBeforeRootSql() throws Exception {
        var f = new Fixture(logging);
        for (Operator predicate : List.of(Operator.EQUAL, Operator.IN)) {
            f.clear();
            var child = f.lines().where(field(), predicate, "FIRST-SECRET");
            child.topNProbeParentThreshold(0);
            var root = f.documents().intent("FIRST-SECRET");
            root.enhanceRelation("lines", child);
            var rows = f.run(root);
            assertEquals(2, rows.size());
            assertEquals(1, ((SmartList<?>) rows.get(0).getProperty("lines")).size());
            // This native Row fixture has no generated reverse-list empty initializer.
            var absent = (SmartList<?>) rows.get(1).getProperty("lines");
            assertTrue(absent == null || absent.isEmpty());
            assertFalse(f.binds.get(0).values().contains("FIRST-SECRET"));
            assertTrue(f.binds.get(1).values().contains("FIRST-SECRET"));
            assertProjection(f, root, "FIRST-SECRET", marked, 2);
            f.clear();
            var independent = f.documents().intent("FIRST-SECRET");
            assertEquals(2, f.run(independent).size());
            assertProjection(f, independent, "FIRST-SECRET", false, 1);
        }
    }

    @Test public void futureAggregateEqualityAndSetOperandsArePrivateBeforeRootSql() throws Exception {
        var f = new Fixture(logging);
        for (Operator predicate : List.of(Operator.EQUAL, Operator.IN)) {
            f.clear();
            var count = f.lines().where(field(), predicate, "FIRST-SECRET");
            count.setPartitionProperty("document"); count.count("count");
            var root = f.documents().intent("FIRST-SECRET");
            root.addSingleAggregateDynamicProperty("selectedLineCount", count);
            var rows = f.run(root);
            assertEquals(2, rows.size());
            assertEquals(1, ((Number) rows.get(0).getDynamicProperty("selectedLineCount")).intValue());
            assertEquals(0, ((Number) rows.get(1).getDynamicProperty("selectedLineCount")).intValue());
            assertFalse(f.binds.get(0).values().contains("FIRST-SECRET"));
            assertTrue(f.binds.get(1).values().contains("FIRST-SECRET"));
            assertEquals(List.of("lines"), f.last.statements().get(1).getTraceChain().stream()
                    .filter(n -> n.getKind() == TraceKind.RELATION).map(TraceNode::getName).toList());
            assertProjection(f, root, "FIRST-SECRET", marked, 2);
        }
    }
}
