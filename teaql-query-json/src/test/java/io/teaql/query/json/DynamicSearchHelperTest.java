package io.teaql.query.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.teaql.core.BaseRequest;
import io.teaql.core.Entity;
import java.util.Optional;
import org.junit.Test;

public class DynamicSearchHelperTest {

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
        public Optional<BaseRequest> subRequestOfFieldName(String fieldName) {
            if ("customer".equals(fieldName) && child != null) {
                return Optional.of(child);
            }
            throw new IllegalArgumentException("unknown relation");
        }
    }
}
