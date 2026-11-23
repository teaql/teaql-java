package io.teaql.core;

import io.teaql.core.criteria.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class QueryCriteriaAlternativesTest {
    private static BaseRequest.TempRequest request() {
        return new BaseRequest.TempRequest(BaseEntity.class, "School");
    }
    private static BaseRequest.TempRequest live() {
        var request = request();
        request.appendSearchCriteria(request.createBasicSearchCriteria("version", Operator.GREATER_THAN, 0L));
        return request;
    }
    private static SearchCriteria condition(String field, long value) {
        return new EQ(new PropertyReference(field), new Parameter(field, value, Operator.EQUAL));
    }
    private static BaseRequest.TempRequest alternatives() {
        var request = live();
        request.appendSearchCriteria(condition("capacity", 100));
        request.appendSearchCriteria(condition("id", 1));
        return request;
    }
    private static void assertAlternatives(Expression expression) {
        assertTrue(expression instanceof OR);
        assertEquals(List.of("capacity", "id"), expression.properties(null));
    }

    @Test public void versionOnlyReceiverRetainsItsGuardAndAddsAnOrGroup() {
        var receiver = live();var guard = receiver.getSearchCriteria();var source = alternatives();
        receiver.internalMatchAny(source);
        var combined = (AND) receiver.getSearchCriteria();
        assertSame(guard, combined.first());assertAlternatives(combined.second());
        assertEquals(3, ((AND) source.getSearchCriteria()).getExpressions().size());
    }

    @Test public void receiverWithoutCriteriaStillAppliesTheAlternatives() {
        var receiver = request();receiver.internalMatchAny(alternatives());
        assertAlternatives(receiver.getSearchCriteria());
    }

    @Test public void aSingleExistingConditionIsNotCastToAnd() {
        var receiver = request();var existing = condition("owner", 42);receiver.appendSearchCriteria(existing);
        receiver.internalMatchAny(alternatives());
        var combined = (AND) receiver.getSearchCriteria();
        assertSame(existing, combined.first());assertAlternatives(combined.second());
    }

    @Test public void emptyOrVersionOnlySourceIsANoOpRatherThanAnEmptyOr() {
        var receiver = live();var before = receiver.getSearchCriteria();
        receiver.internalMatchAny(request());receiver.internalMatchAny(live());
        var onlyVersions = live();
        onlyVersions.appendSearchCriteria(onlyVersions.createBasicSearchCriteria("version", Operator.EQUAL, 99L));
        receiver.internalMatchAny(onlyVersions);assertSame(before, receiver.getSearchCriteria());
    }

    @Test public void explicitNestedBooleanGroupsAreNotFlattenedIntoAlternatives() {
        var source = live();var nested = SearchCriteria.and(condition("capacity", 100), condition("owner", 42));
        source.appendSearchCriteria(nested);source.appendSearchCriteria(condition("id", 1));
        var receiver = live();receiver.internalMatchAny(source);
        var alternatives = (OR) ((AND) receiver.getSearchCriteria()).second();
        assertEquals(2, alternatives.getExpressions().size());assertSame(nested, alternatives.first());
    }

    @Test public void nonAndSourceIsOneConditionAndNeverCopiesItsPageOrMeasures() {
        var source = request();var filter = condition("capacity", 100);source.appendSearchCriteria(filter);
        source.groupBy("id");source.count("count");source.top(0);
        var receiver = live();var beforeSlice = receiver.getSlice();receiver.internalMatchAny(source);
        assertSame(filter, ((AND) receiver.getSearchCriteria()).second());
        assertSame(beforeSlice, receiver.getSlice());assertTrue(receiver.getAggregations().getAggregates().isEmpty());
    }
}
