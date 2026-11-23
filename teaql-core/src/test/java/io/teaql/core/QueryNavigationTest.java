package io.teaql.core;

import io.teaql.core.criteria.Operator;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class QueryNavigationTest {
    public static class Row extends BaseEntity { @Override public String typeName() { return "School"; } }
    static class Request extends BaseRequest<Row> {
        Request() { super(Row.class); }
        @Override public String getTypeName() { return "School"; }
        @Override protected BaseRequest<Row> internalCopyTarget() { return new Request(); }
        @Override public Request copy() { return (Request) super.copy(); }
        void navigate(SearchRequest<?> parent) { internalRollUpTo("kind", parent); }
    }
    static List<SubQuerySearchCriteria> memberships(SearchCriteria criteria) {
        var result = new ArrayList<SubQuerySearchCriteria>();
        if (criteria instanceof SubQuerySearchCriteria membership) result.add(membership);
        else if (criteria instanceof FunctionApply function)
            for (var operand : function.getExpressions()) if (operand instanceof SearchCriteria condition) result.addAll(memberships(condition));
        return result;
    }

    @Test public void latestTargetReplacesItsMembershipAndDimension() {
        var source=new Request();var first=new Request();var second=new Request();
        source.navigate(first);source.navigate(second);
        assertSame(second,source.getPropagateDimensions().get("kind"));
        assertEquals(1,source.getAggregations().getComplexDimensions().size());
        assertEquals(1,memberships(source.getSearchCriteria()).size());
        assertSame(second,memberships(source.getSearchCriteria()).get(0).getDependsOn());
    }
    @Test public void copiedNavigationDoesNotRebindTheOriginal() {
        var source=new Request();var first=new Request();source.navigate(first);
        var copy=source.copy();var replacement=new Request();copy.navigate(replacement);
        assertSame(first,source.getPropagateDimensions().get("kind"));
        assertSame(replacement,copy.getPropagateDimensions().get("kind"));
        assertEquals(1,memberships(copy.getSearchCriteria()).size());
    }
    @Test public void anIdenticalBusinessPredicateIsNotOwnedByNavigation() {
        var source=new Request();var parent=new Request();
        source.appendSearchCriteria(new SubQuerySearchCriteria("kind",parent,"id"));source.navigate(parent);
        var copy=source.copy();var replacement=new Request();copy.navigate(replacement);
        var members=memberships(copy.getSearchCriteria());assertEquals(2,members.size());
        assertEquals(1,members.stream().filter(SubQuerySearchCriteria::navigationOwned).count());
        assertTrue(members.stream().anyMatch(member->!member.navigationOwned() && member.getDependsOn()!=replacement));
    }
    @Test public void alternativesRetainUnrelatedFieldsForNormalValidation() {
        var request=new Request();var unknown=request.createBasicSearchCriteria("unknown_logical_field",Operator.EQUAL,1L);
        var owned=new SubQuerySearchCriteria("kind",new Request(),"id",true);
        var result=QueryNavigation.clear(SearchCriteria.or(owned,unknown),"kind",0);
        assertTrue(((FunctionApply)result).getExpressions().contains(unknown));
        assertEquals(2,((FunctionApply)result).getExpressions().size());
    }
    @Test public void repeatedNavigationDoesNotAccumulateAstNodes() {
        var source=new Request();for(int index=0;index<100;index++)source.navigate(new Request());
        assertEquals(1,source.getAggregations().getComplexDimensions().size());
        assertEquals(1,memberships(source.getSearchCriteria()).size());
    }
    @Test public void rejectedRebindingLeavesTheOriginalCriteriaUntouched() {
        SearchCriteria criteria=new SubQuerySearchCriteria("kind",new Request(),"id",true);
        for(int depth=0;depth<65;depth++)criteria=SearchCriteria.not(criteria);
        var source=new Request();source.replaceSearchCriteria(criteria);var before=source.getSearchCriteria();
        assertThrows(TeaQLRuntimeException.class,()->source.navigate(new Request()));
        assertSame(before,source.getSearchCriteria());assertTrue(source.getPropagateDimensions().isEmpty());
    }
}
