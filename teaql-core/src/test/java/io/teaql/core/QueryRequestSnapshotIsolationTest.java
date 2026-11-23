package io.teaql.core;

import io.teaql.core.criteria.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class QueryRequestSnapshotIsolationTest {
    private static BaseRequest.TempRequest request(String name) {
        return new BaseRequest.TempRequest(BaseEntity.class, name);
    }

    @Test public void snapshotFreezesMutableGroupingAndHavingOperands() {
        var source = request("School");
        var property = new PropertyReference("capacity");
        var measure = new SimpleNamedExpression("total", new AggrExpression(AggrFunction.SUM, property));
        source.addAggregate(measure);
        var values = new ArrayList<Object>(List.of(1L, 2L));
        var parameter = new Parameter("total", values, Operator.IN);
        source.having(new TwoOperatorCriteria(Operator.IN, new PropertyReference("total"), parameter));
        var frozen = new QueryRequestSnapshot<>(source, null);
        property.setPropertyName("id");
        ((List<Object>) parameter.getValue()).add(3L);
        assertEquals(List.of("capacity"), frozen.getAggregations().getAggregates().get(0).properties(null));
        var saved = (Parameter) ((FunctionApply) frozen.getHaving()).second();
        assertEquals(List.of(1L, 2L), saved.getValue());
    }

    @Test public void snapshotPreservesSharedParentIdentityWithoutSharingTheBuilder() {
        var parent = request("Kind"); parent.groupBy("name");
        var source = request("School"); source.groupBy("kind", parent);
        source.appendSearchCriteria(new SubQuerySearchCriteria("kind", parent, "id"));
        var frozen = new QueryRequestSnapshot<>(source, null);
        var copiedParent = frozen.getPropagateDimensions().get("kind");
        assertNotSame(parent, copiedParent);
        assertSame(copiedParent, ((SubQuerySearchCriteria) frozen.getSearchCriteria()).getDependsOn());
        parent.groupBy("code");
        assertEquals(1, copiedParent.getAggregations().getSimpleDimensions().size());
    }

    @Test public void explicitCopyPreservesTypedNodesAndDetachesEveryMutableAstContainer() {
        var parent = request("Kind"); parent.groupBy("name"); parent.offset(0, 2);
        var source = request("School"); source.groupBy("kind", parent);
        source.appendSearchCriteria(new SubQuerySearchCriteria("kind", parent, "id"));
        source.having("count", Operator.GREATER_THAN, 1L);
        var date = new Date(1234); var counts = new long[]{1L,2L};
        source.setExtensions(new HashMap<>(Map.of("labels", new ArrayList<>(List.of("original")), "date", date, "counts", counts)));
        var frozen = source.copy();
        var child = frozen.getPropagateDimensions().get("kind");
        assertEquals(parent.getClass(), child.getClass());
        assertSame(child, ((SubQuerySearchCriteria) frozen.getSearchCriteria()).getDependsOn());
        parent.getSlice().setSize(9); parent.groupBy("code");
        ((List<String>) source.getExtension("labels")).add("changed");
        date.setTime(9999); counts[0]=99;
        assertEquals(2, child.getSlice().getSize());
        assertEquals(1, child.getAggregations().getSimpleDimensions().size());
        assertEquals(List.of("original"), frozen.getExtension("labels"));
        assertEquals(1234, ((Date) frozen.getExtension("date")).getTime());
        assertArrayEquals(new long[]{1L,2L}, (long[]) frozen.getExtension("counts"));
        assertNotSame(source.getHaving(), frozen.getHaving());
    }

    @Test public void cyclicAndDeepGraphsFailWithoutStackOverflow() {
        var cycle = request("School"); cycle.groupBy("self", cycle);
        assertThrows(TeaQLRuntimeException.class, () -> cycle.copy());
        assertThrows(TeaQLRuntimeException.class, () -> new QueryRequestSnapshot<>(cycle, null));
        var root = request("School"); var current = root;
        for (int i=0; i<70; i++) { var child = request("School"); current.groupBy("next", child); current = child; }
        assertThrows(TeaQLRuntimeException.class, () -> root.copy());
        var expression = new FunctionApply(Operator.SOUNDS_LIKE, new PropertyReference("name"));
        expression.getExpressions().set(0, expression);
        var invalid = request("School"); invalid.getAggregations().getSimpleDimensions().add(new SimpleNamedExpression("key", expression));
        assertThrows(TeaQLRuntimeException.class, () -> invalid.copy());
    }

    @Test public void explicitCopyRejectsUnknownMutableExpressionsAndValuesWithoutEchoingPayloads() {
        var custom = request("School");
        custom.getProjections().add(new SimpleNamedExpression("custom", new Expression() {}));
        assertThrows(TeaQLRuntimeException.class, () -> custom.copy());
        var unknown = request("School");
        unknown.setExtensions(Map.of("secret", new StringBuilder("not-for-diagnostics")));
        var failure = assertThrows(TeaQLRuntimeException.class, () -> unknown.copy());
        assertFalse(failure.getMessage().contains("not-for-diagnostics"));
        unknown.setExtensions(Map.of("mutableNumber", new java.util.concurrent.atomic.AtomicLong(1)));
        assertThrows(TeaQLRuntimeException.class, () -> unknown.copy());
        unknown.setExtensions(Map.of("clock", java.time.Clock.systemUTC()));
        assertThrows(TeaQLRuntimeException.class, () -> unknown.copy());
    }
}
