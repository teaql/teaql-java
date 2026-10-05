package io.teaql.sqlite;

import io.teaql.core.*;
import io.teaql.core.criteria.*;
import io.teaql.runtime.LogPrivacy;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Built-in BETWEEN/phonetic operands must be classified before any future child SQL. */
@RunWith(Parameterized.class)
public class TypedIntentPrivacySqliteTest {
    @Parameterized.Parameters(name="{0}, marked={1}, logging={2}")
    public static Collection<Object[]> cases() {
        var cases = new ArrayList<Object[]>();
        for (var op : List.of(Operator.BETWEEN, Operator.SOUNDS_LIKE))
            for (boolean marked : List.of(false, true))
                for (boolean logging : List.of(false, true)) cases.add(new Object[]{op, marked, logging});
        return cases;
    }

    private final Operator op;
    private final boolean marked, logging;
    public TypedIntentPrivacySqliteTest(Operator op, boolean marked, boolean logging) {
        this.op = op; this.marked = marked; this.logging = logging;
    }
    private String field() { return marked ? "name" : "state"; }
    private List<String> operands(String prefix) {
        return op == Operator.BETWEEN ? List.of(prefix + "-SECRET", prefix + "-SECREZ")
                : List.of(prefix + "-SECRET");
    }
    private String intent(List<String> operands) { return String.join(" / ", operands); }
    private void predicate(LikeIntentPrivacySqliteTest.Request<?> request, List<String> operands) {
        request.appendSearchCriteria(request.createBasicSearchCriteria(field(), op, operands.toArray()));
    }
    private void assertPath(ExecutionMetadata entry, boolean child) {
        var expected = new ArrayList<>(List.of(
                new TraceNode(TraceKind.OPERATION, "TraceDocument", null, "query"),
                new TraceNode(TraceKind.REQUEST, "TraceDocument", null, "")));
        if (child) expected.add(new TraceNode(TraceKind.RELATION, "lines", null, "TraceDocument.lines"));
        expected.add(new TraceNode(TraceKind.PROVIDER, "sqlite", null, ""));
        expected.add(new TraceNode(TraceKind.SQL, "select", null, ""));
        assertEquals(expected, entry.getTraceChain());
    }
    private void assertProjection(LikeIntentPrivacySqliteTest.Fixture f,
            LikeIntentPrivacySqliteTest.Request<?> root, List<String> operands, boolean privateOperand, int count) {
        assertEquals(count, f.binds.size());
        assertEquals(count, f.last.statements().size());
        assertEquals(logging ? count : 0, f.safe.size());
        String text = intent(operands);
        String projected = privateOperand ? String.join(" / ", Collections.nCopies(operands.size(), "[REDACTED]")) : text;
        assertEquals("inspect " + text, root.comment());
        assertEquals("render " + text, root.purpose());
        assertEquals(root.comment(), f.policies.get(0).comment());
        assertEquals(root.purpose(), f.policies.get(0).purpose());
        for (int i = 0; i < count; i++) {
            var raw = f.last.statements().get(i);
            assertPath(raw, i > 0);
            assertEquals(root.comment(), raw.getComment());
            assertEquals(root.purpose(), raw.getPurpose());
            var safe = LogPrivacy.sql(raw, false);
            assertEquals("inspect " + projected, safe.getComment());
            assertEquals("render " + projected, safe.getPurpose());
            assertPath(safe, i > 0);
            assertNull(safe.getIntentRedactions());
            // Explicit debugging may reveal marked business operands, never by mutating raw intent.
            assertEquals(root.comment(), LogPrivacy.sql(raw, true).getComment());
            if (logging) {
                assertEquals(safe.getComment(), f.safe.get(i).getComment());
                assertEquals(safe.getPurpose(), f.safe.get(i).getPurpose());
                assertPath(f.safe.get(i), i > 0);
                assertNull(f.safe.get(i).getIntentRedactions());
            }
        }
        if (logging && privateOperand)
            for (String operand : operands) assertFalse(f.output.toString().contains(operand));
        if (!logging) assertEquals("", f.output.toString());
        assertTrue(f.context.getTraceChain().isEmpty());
    }
    private void independent(LikeIntentPrivacySqliteTest.Fixture f, List<String> operands) {
        f.clear();
        var request = f.documents().intent(intent(operands));
        assertEquals(2, f.run(request).size());
        assertProjection(f, request, operands, false, 1);
    }
    private void assertBinds(LikeIntentPrivacySqliteTest.Fixture f, List<String> operands, int index) {
        for (String operand : operands) assertTrue(f.binds.get(index).values().contains(operand));
        assertTrue(f.binds.get(index).sql().toUpperCase(Locale.ROOT)
                .contains(op == Operator.BETWEEN ? " BETWEEN " : "SOUNDEX("));
    }

    @Test public void typedRootAndCachedRebindingKeepOriginalOperandsPrivate() throws Exception {
        var f = new LikeIntentPrivacySqliteTest.Fixture(logging);
        for (String prefix : List.of("FIRST", "SECOND")) {
            f.clear();
            var values = operands(prefix);
            var root = f.documents().intent(intent(values)); predicate(root, values);
            var rows = f.run(root);
            assertEquals(1, rows.size());
            assertEquals(prefix.equals("FIRST") ? 1L : 2L, rows.get(0).getId().longValue());
            assertBinds(f, values, 0);
            assertProjection(f, root, values, marked, 1);
        }
        independent(f, operands("FIRST"));
    }

    @Test public void futureChildOperandsArePrivateBeforeFirstRootSql() throws Exception {
        var f = new LikeIntentPrivacySqliteTest.Fixture(logging);
        var values = operands("FIRST");
        var child = f.lines(); predicate(child, values); child.topNProbeParentThreshold(0);
        var root = f.documents().intent(intent(values)); root.enhanceRelation("lines", child);
        var rows = f.run(root);
        assertEquals(2, rows.size());
        var members = (SmartList<?>) rows.get(0).getProperty("lines");
        assertEquals(1, members.size()); assertEquals(Long.valueOf(11), members.get(0).getId());
        assertEquals(Long.valueOf(1), ((Entity) members.get(0).getProperty("document")).getId());
        var absent = (SmartList<?>) rows.get(1).getProperty("lines");
        assertTrue(absent == null || absent.isEmpty());
        for (String value : values) assertFalse(f.binds.get(0).values().contains(value));
        assertBinds(f, values, 1);
        assertProjection(f, root, values, marked, 2);
        independent(f, values);
    }

    @Test public void futureAggregateOperandsArePrivateBeforeFirstRootSql() throws Exception {
        var f = new LikeIntentPrivacySqliteTest.Fixture(logging);
        var values = operands("FIRST");
        var count = f.lines(); predicate(count, values);
        count.setPartitionProperty("document"); count.count("count");
        var root = f.documents().intent(intent(values)); root.addSingleAggregateDynamicProperty("selectedLineCount", count);
        var rows = f.run(root);
        assertEquals(2, rows.size());
        assertEquals(1, ((Number) rows.get(0).getDynamicProperty("selectedLineCount")).intValue());
        assertEquals(0, ((Number) rows.get(1).getDynamicProperty("selectedLineCount")).intValue());
        for (String value : values) assertFalse(f.binds.get(0).values().contains(value));
        assertBinds(f, values, 1);
        assertTrue(f.binds.get(1).sql().toLowerCase(Locale.ROOT).contains("count("));
        assertProjection(f, root, values, marked, 2);
        independent(f, values);
    }

    @Test public void parameterNamesCannotReplaceTheResolvedFieldPolicy() throws Exception {
        var f = new LikeIntentPrivacySqliteTest.Fixture(logging);
        var values = operands("FIRST");
        var child = f.lines();
        // Deliberately opposite metadata names: only the property reference determines field policy.
        String parameterName = marked ? "state" : "name";
        SearchCriteria criterion = op == Operator.BETWEEN
                ? new Between(new PropertyReference(field()), new Parameter(parameterName, values.get(0), op),
                        new Parameter(parameterName, values.get(1), op))
                : new EQ(new FunctionApply(op, new PropertyReference(field())),
                        new FunctionApply(op, new Parameter(parameterName, values.get(0), op)));
        child.appendSearchCriteria(criterion); child.topNProbeParentThreshold(0);
        var root = f.documents().intent(intent(values)); root.enhanceRelation("lines", child);
        assertEquals(2, f.run(root).size());
        assertBinds(f, values, 1);
        assertProjection(f, root, values, marked, 2);
        independent(f, values);
    }

    @Test public void rewrittenParameterOperatorsDoNotInventOriginalOperands() throws Exception {
        var f = new LikeIntentPrivacySqliteTest.Fixture(logging);
        var values = operands("FIRST");
        var child = f.lines();
        var rewritten = new Parameter(field(), values.get(0), Operator.CONTAIN);
        SearchCriteria criterion = op == Operator.BETWEEN
                ? new Between(new PropertyReference(field()), rewritten, new Parameter(field(), values.get(1), op))
                : new EQ(new FunctionApply(op, new PropertyReference(field())), new FunctionApply(op, rewritten));
        child.appendSearchCriteria(criterion); child.topNProbeParentThreshold(0);
        var root = f.documents().intent(values.get(0)); root.enhanceRelation("lines", child);
        assertEquals(2, f.run(root).size());
        assertTrue(f.binds.get(1).values().contains("%" + values.get(0) + "%"));
        assertFalse(f.binds.get(1).values().contains(values.get(0)));
        assertProjection(f, root, List.of(values.get(0)), false, 2);
        assertEquals(values.get(0), rewritten.getValue());
        assertEquals(Operator.CONTAIN, rewritten.getOperator());
    }

    @Test public void credentialOperandsStayPrivateEvenInExplicitDebugProjection() throws Exception {
        var f = new LikeIntentPrivacySqliteTest.Fixture(logging);
        var values = operands("FIRST");
        var child = f.lines();
        SearchCriteria criterion = op == Operator.BETWEEN
                ? new Between(new PropertyReference(field()), new Parameter("apiKey", values.get(0), op),
                        new Parameter("apiKey", values.get(1), op))
                : new EQ(new FunctionApply(op, new PropertyReference(field())),
                        new FunctionApply(op, new Parameter("apiKey", values.get(0), op)));
        child.appendSearchCriteria(criterion); child.topNProbeParentThreshold(0);
        var root = f.documents().intent(intent(values)); root.enhanceRelation("lines", child);
        assertEquals(2, f.run(root).size());
        assertBinds(f, values, 1);
        String projected = String.join(" / ", Collections.nCopies(values.size(), "[REDACTED]"));
        assertEquals(2, f.last.statements().size());
        assertEquals(logging ? 2 : 0, f.safe.size());
        for (int i = 0; i < 2; i++) {
            var raw = f.last.statements().get(i); assertPath(raw, i > 0);
            assertEquals(root.comment(), raw.getComment()); assertEquals(root.purpose(), raw.getPurpose());
            for (boolean allowPlaintext : List.of(false, true)) {
                var safe = LogPrivacy.sql(raw, allowPlaintext);
                assertEquals("inspect " + projected, safe.getComment());
                assertEquals("render " + projected, safe.getPurpose());
                assertPath(safe, i > 0); assertNull(safe.getIntentRedactions());
            }
            if (logging) assertEquals("inspect " + projected, f.safe.get(i).getComment());
        }
        for (String value : values) assertFalse(f.output.toString().contains(value));
        assertTrue(f.context.getTraceChain().isEmpty());
        independent(f, values);
    }
}
