package io.teaql.runtime;

import io.teaql.core.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class QueryRequestTreeTest {
    static class Request extends BaseRequest<TeaQLRuntimeTest.DummyEntity> {
        private final String type;
        Request(String type, String comment, String purpose) {
            super(TeaQLRuntimeTest.DummyEntity.class, TeaQLRuntimeTest.DummyEntity::new);
            this.type = type; internalComment(comment); internalPurpose(purpose);
        }
        @Override public String getTypeName() { return type; }
    }
    private DefaultQueryRequest root() { return new DefaultQueryRequest(new Request("School", "load schools", "render school list")); }
    private List<String> comments(DefaultQueryRequest request) { return request.businessTraceChain().stream().map(TraceNode::getComment).toList(); }
    private QueryOperation relation() { return QueryOperation.nativeMethod(QueryOperation.Kind.RELATION_SELECT, "selectSchoolTypeWith", null, "schoolType"); }

    @Test public void childResolvesDirectParentAndRootWithoutCopyingParentBody() {
        var root = root();
        var child = root.derive(new Request("SchoolType", "load visible types", null), relation());
        assertSame(root.requestScope(), child.parentRequest().parentRequest());
        assertSame(root.requestScope(), child.rootRequest());
        assertEquals(List.of("render school list", "load schools", "selectSchoolTypeWith(...)", "load visible types"), comments(child));
        assertEquals("load schools", child.comment());
        assertEquals("load visible types", child.localComment());
    }
    @Test public void defaultsCannotAuthorizeAnInvalidRootOrReplaceExplicitBlank() {
        assertEquals("load SchoolType", root().derive(new Request("SchoolType", null, null), relation()).localComment());
        assertThrows(RequestIntentException.class, () -> new DefaultQueryRequest(new Request("School", null, "render schools")));
        for (String blank : List.of("", " \n ", "\u0085"))
            assertThrows(RequestIntentException.class, () -> root().derive(new Request("SchoolType", blank, null), relation()));
    }
    @Test public void replacementAndRecaptureRetainSharedHeaderAndRoot() {
        var root = root(); var child = root.derive(new Request("SchoolType", "load types", null), relation());
        var copy = child.withQuery(new Request("SchoolType", null, null));
        assertSame(child.requestScope(), copy.requestScope());
        assertSame(root.requestScope(), new DefaultQueryRequest(copy.getSearchRequest()).rootRequest());
    }
    @Test public void sharedTemplateGetsDistinctOwnersAndMutableContainersAreCopied() {
        var template = new Request("SchoolType", "load types", null);
        template.getSlice().setSize(9); template.selectProperty("name");
        var first = root().derive(template, relation()); var second = root().derive(template, relation());
        assertNotSame(first.rootRequest(), second.rootRequest());
        first.getSearchRequest().getSlice().setSize(3);
        ((BaseRequest<?>) first.getSearchRequest()).selectProperty("id");
        assertEquals(9, template.getSlice().getSize());
        assertEquals(9, second.getSearchRequest().getSlice().getSize());
        assertEquals(1, template.getProjections().size()); assertNull(template.requestScope());
    }
    @Test public void unrelatedInvocationCannotReplaceOwnedQueryEvenWithIdenticalIntent() {
        var first = root(); var second = root();
        assertThrows(IllegalArgumentException.class, () -> first.withQuery(second.getSearchRequest()));
        var ownCopy = first.withQuery(first.getSearchRequest());
        assertSame(first.requestScope(), ownCopy.withQuery(first.getSearchRequest()).requestScope());
        assertThrows(IllegalArgumentException.class, () -> ownCopy.withQuery(second.getSearchRequest()));
    }
    @Test public void countAliasUsesCentralJavaLiteralRenderer() {
        var child = root().deriveOperation(new Request("School", null, null),
                QueryOperation.nativeMethod(QueryOperation.Kind.NAMED_COUNT, "countAs", "total\"\\schools\n", null));
        assertEquals("countAs(\"total\\\"\\\\schools\\n\")", child.localComment());
        assertTrue(child.getSearchRequest().sqlTraceSource().isEmpty());
    }
    @Test public void prefixesAreOrdinaryTextAndNoOwnerAppearsInQueryExtensions() {
        var request = new DefaultQueryRequest(new Request("School", "what: load schools", "why: render schools"));
        assertEquals(List.of("why: render schools", "what: load schools"), comments(request));
        assertTrue(request.getSearchRequest().getExtensions().isEmpty());
    }
    @Test public void relationHelpersRetainCallerSqlAliasesWithoutMutatingTemplates() {
        var parent = new Request("School", "load schools", "render schools");
        var template = new Request("SchoolType", "load types", null);
        template.selectProperty("name");
        parent.addRelationCount("typeCount", template, "school", QueryOperation.nativeMethod(
                QueryOperation.Kind.RELATION_COUNT, "countSchoolTypesAs", "typeCount", "types"));
        parent.addRelationScalarAggregate("typeTotal", template, "school", "amount", AggrFunction.SUM,
                QueryOperation.nativeMethod(QueryOperation.Kind.AGGREGATE, "sumAmountOfSchoolTypesAs", "typeTotal", "types"));
        var count = parent.getDynamicAggregateAttributes().get(0).getAggregateRequest();
        var sum = parent.getDynamicAggregateAttributes().get(1).getAggregateRequest();
        assertEquals("typeCount", count.getAggregations().getAggregates().get(0).name());
        assertEquals("typeTotal", sum.getAggregations().getAggregates().get(0).name());
        assertTrue(template.getAggregations().getAggregates().isEmpty());
        assertNull(template.getPartitionProperty()); assertEquals(1, template.getProjections().size());
    }
}
