package io.teaql.query.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertSame;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.teaql.core.BaseRequest;
import io.teaql.core.Entity;
import java.util.Optional;
import org.junit.Test;

public class DynamicSearchHelperTest {

    @Test
    public void invalidPagingCannotChangeTrustedHardLimitOrFilters() {
        for (String paging : new String[] {"\"_size\":10001", "\"_size\":-1",
                "\"_pageSize\":0", "\"_start\":1.5", "\"_size\":\"10\""}) {
            StubRequest request = new StubRequest("Order");
            int hardLimit = request.hardLimit();
            assertThrows(IllegalArgumentException.class, () -> new DynamicSearchHelper().mergeClauses(
                    request, DynamicSearchHelper.jsonFromString("{\"name\":\"valid\"," + paging + "}")));
            assertEquals(hardLimit, request.hardLimit());
            assertTrue(request.getSearchCriteria() == null);
        }
    }

    @Test
    public void unsupportedOperatorAndBadReferenceIdAreNotNullPredicates() {
        for (String value : new String[] {"{\"$invalid\":1}", "{\"id\":\"not-an-id\"}"}) {
            StubRequest request = new StubRequest("Order");
            assertThrows(IllegalArgumentException.class, () -> new DynamicSearchHelper().mergeClauses(
                    request, DynamicSearchHelper.jsonFromString("{\"name\":" + value + "}")));
            assertTrue(request.getSearchCriteria() == null);
            assertTrue(DynamicSearchHelper.warningsOf(request).isEmpty());
        }
    }

    @Test
    public void malformedAndNonObjectInputRemainsFatalWithoutEchoingValues() {
        for (String input : new String[] {"{secret", "[]", "null", "42", "\"secret\"", "{} {}"}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> DynamicSearchHelper.jsonFromString(input));
            assertTrue(!error.getMessage().contains("secret"));
        }
    }

    @Test
    public void reservedContextControlsFailBeforeChangingRequest() {
        for (String control : new String[] {"_tenant", "_principal", "_policy", "_audit", "_hardLimit"}) {
            StubRequest request = new StubRequest("Order");
            assertThrows(IllegalArgumentException.class, () -> new DynamicSearchHelper().mergeClauses(
                    request, DynamicSearchHelper.jsonFromString(
                            "{\"name\":\"valid\",\"" + control + "\":\"secret\"}")));
            assertTrue(request.getSearchCriteria() == null);
            assertTrue(DynamicSearchHelper.warningsOf(request).isEmpty());
        }
    }

    @Test
    public void staleClauseKeepsValidSiblingAndWarningOmitsItsValue() throws Exception {
        StubRequest request = new StubRequest("Order");
        DynamicSearchHelper helper = new DynamicSearchHelper();
        helper.mergeClauses(request, DynamicSearchHelper.jsonFromString("{\"name\":\"valid\"}"));
        Object valid = request.getSearchCriteria();
        helper.mergeClauses(request, DynamicSearchHelper.jsonFromString("{\"removed\":\"SECRET_VALUE\"}"));
        assertSame(valid, request.getSearchCriteria());
        String warnings = new ObjectMapper().writeValueAsString(DynamicSearchHelper.warningsOf(request));
        assertTrue(!warnings.contains("SECRET_VALUE"));
        assertWarning(request, "FILTER", "removed");
    }

    @Test
    public void unknownTopLevelFilterIsIgnoredAndRecorded() {
        StubRequest request = new StubRequest("Order");

        new DynamicSearchHelper()
                .mergeClauses(
                        request,
                        DynamicSearchHelper.jsonFromString("{\"removedField\":\"value\"}"));

        assertTrue(request.getSearchCriteria() == null);
        assertWarning(request, "FILTER", "removedField");
    }

    @Test
    public void unknownRelationOrNestedFieldDoesNotThrow() {
        StubRequest request = new StubRequest("Order");
        request.child = new StubRequest("Customer");

        DynamicSearchHelper helper = new DynamicSearchHelper();
        helper.mergeClauses(
                request,
                DynamicSearchHelper.jsonFromString("{\"removedRelation.name\":\"x\"}"));
        helper.mergeClauses(
                request,
                DynamicSearchHelper.jsonFromString("{\"customer.removedField\":\"x\"}"));

        assertEquals(2, DynamicSearchHelper.warningsOf(request).size());
        assertTrue("stale nested fields must not leave a partial relation filter", request.getSearchCriteria() == null);
        assertWarning(request, "FILTER", "removedRelation.name");
        assertWarning(request, "FILTER", "customer.removedField");
    }

    @Test
    public void unknownOrderFieldsAreIgnoredAndRecorded() {
        StubRequest request = new StubRequest("Order");

        new DynamicSearchHelper()
                .mergeClauses(
                        request,
                        DynamicSearchHelper.jsonFromString(
                                "{\"_orderBy\":[{\"field\":\"oldName\",\"useAsc\":true}]}"));

        assertTrue(request.getOrderBy().isEmpty());
        assertWarning(request, "ORDER_BY", "oldName");
    }

    private static void assertWarning(BaseRequest<?> request, String clause, String fieldPath) {
        assertTrue(
                DynamicSearchHelper.warningsOf(request).stream()
                        .anyMatch(
                                warning ->
                                        DynamicSearchWarning.UNKNOWN_FIELD.equals(warning.getCode())
                                                && clause.equals(warning.getClause())
                                                && fieldPath.equals(warning.getFieldPath())));
    }

    private static final class StubRequest extends BaseRequest<Entity> {
        private final String typeName;
        private StubRequest child;

        private StubRequest(String typeName) {
            super(Entity.class);
            this.typeName = typeName;
        }

        @Override
        public String getTypeName() {
            return typeName;
        }

        @Override
        public boolean isOneOfSelfField(String propertyName) {
            return "id".equals(propertyName) || "name".equals(propertyName);
        }

        @Override
        public boolean isDateTimeField(String fieldName) {
            return false;
        }

        @Override
        public Optional<BaseRequest> subRequestOfFieldName(String fieldName) {
            if ("customer".equals(fieldName) && child != null) {
                return Optional.of(child);
            }
            throw new IllegalArgumentException("unknown relation");
        }
    }
}
