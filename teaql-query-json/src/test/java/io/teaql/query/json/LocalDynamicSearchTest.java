package io.teaql.query.json;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.Test;
import io.teaql.core.BaseRequest;
import io.teaql.core.Entity;
import io.teaql.core.OrderBy;
import io.teaql.core.SearchCriteria;
import io.teaql.core.criteria.AND;
import io.teaql.core.criteria.Operator;

public class LocalDynamicSearchTest {
    private static final class SchoolRequest extends BaseRequest<Entity> {
        SchoolRequest() { super(Entity.class); }
        @Override public String getTypeName() { return "School"; }
    }

    @Test public void mergePreservesBasisAndDoesNotModifySharedAnd() {
        SchoolRequest request = new SchoolRequest();
        AND trusted = new AND(request.createBasicSearchCriteria("id", Operator.GREATER_THAN, 0L));
        request.replaceSearchCriteria(trusted);
        request.addOrderBy("id", false);
        request.setSize(7);
        int hardLimit = request.hardLimit();
        var previousOrders = request.getOrderBy().getOrderBys();
        SearchCriteria bound = request.createBasicSearchCriteria("name", Operator.EQUAL, "x");
        var result = LocalDynamicSearch.merge(request,
                "{\"filter\":{\"name\":\"x\",\"removed\":1},\"orderBy\":[{\"field\":\"name\",\"direction\":\"asc\"}]}",
                MODELS, filter -> bound, order -> new OrderBy(order.fieldPath(), "ASC"), warning -> {});
        AND combined = (AND) request.getSearchCriteria();
        assertEquals(2, combined.getExpressions().size());
        assertSame(trusted, combined.getExpressions().get(0));
        assertSame(bound, combined.getExpressions().get(1));
        assertEquals(1, trusted.getExpressions().size());
        assertEquals(1, previousOrders.size());
        assertEquals(2, request.getOrderBy().getOrderBys().size());
        assertEquals(7, request.getSize());
        assertEquals(hardLimit, request.hardLimit());
        assertEquals(1, result.warnings().size());
    }

    @Test public void bindingFailureDoesNotChangeRequestOrEmitWarnings() {
        SchoolRequest request = new SchoolRequest();
        SearchCriteria original = request.createBasicSearchCriteria("id", Operator.GREATER_THAN, 0L);
        request.replaceSearchCriteria(original);
        var originalOrders = request.getOrderBy().getOrderBys();
        var warnings = new ArrayList<LocalDynamicSearch.Warning>();
        assertThrows(IllegalStateException.class, () -> LocalDynamicSearch.merge(request,
                "{\"filter\":{\"name\":\"x\",\"removed\":1},\"orderBy\":[{\"field\":\"id\",\"direction\":\"asc\"}]}",
                MODELS, filter -> request.createBasicSearchCriteria("name", Operator.EQUAL, "x"),
                order -> { throw new IllegalStateException("binding failed"); }, warnings::add));
        assertSame(original, request.getSearchCriteria());
        assertSame(originalOrders, request.getOrderBy().getOrderBys());
        assertTrue(warnings.isEmpty());
    }
    private static final Map<String, LocalDynamicSearch.Model> MODELS = Map.of("School",
            new LocalDynamicSearch.Model(Map.of("id", "integer", "name", "string",
                    "amount", "decimal", "date", "date"), Map.of()));

    @Test public void preservesValidSiblingsAndExactDecimal() {
        var warnings = new ArrayList<LocalDynamicSearch.Warning>();
        var result = LocalDynamicSearch.normalize(
                "{\"filter\":{\"removed\":\"SECRET\",\"id\":1.0,\"amount\":\"12345678901234567890.123\"}}",
                "School", MODELS, warnings::add);
        assertEquals(2, result.filters().size());
        assertEquals("12345678901234567890.123", result.filters().get(1).value().textValue());
        assertEquals(1, warnings.size());
        assertEquals("removed", warnings.get(0).fieldPath());
    }

    @Test public void fatalSiblingsEmitNoWarnings() {
        for (String value : new String[] {"true", "1.5", "9007199254740992", "{\"$invalid\":1}"}) {
            var warnings = new ArrayList<LocalDynamicSearch.Warning>();
            assertThrows(IllegalArgumentException.class, () -> LocalDynamicSearch.normalize(
                    "{\"filter\":{\"removed\":\"SECRET\",\"id\":" + value + "}}",
                    "School", MODELS, warnings::add));
            assertTrue(warnings.isEmpty());
        }
    }

    @Test public void resourceLimitsAndMalformedPathsRemainFatal() {
        assertThrows(IllegalArgumentException.class, () -> LocalDynamicSearch.normalize(
                "{\"filter\":{\"id\":1,\"name\":\"x\"}}", "School", MODELS, null, 1));
        assertThrows(IllegalArgumentException.class, () -> LocalDynamicSearch.normalize(
                "{\"filter\":{\"a..b\":1}}", "School", MODELS, null));
        assertThrows(IllegalArgumentException.class, () -> LocalDynamicSearch.normalize(
                "{\"filter\":{\"date\":\"0000-01-01\"}}", "School", MODELS, null));
        assertThrows(IllegalArgumentException.class, () -> LocalDynamicSearch.normalize(
                "{\"filter\":{\"amount\":\"١٢\"}}", "School", MODELS, null));
    }

    @Test public void warningLoggingIsEnabledWithoutCallback() {
        Logger logger = Logger.getLogger(LocalDynamicSearch.class.getName());
        AtomicInteger count = new AtomicInteger();
        Handler handler = new Handler() {
            public void publish(LogRecord record) {
                assertFalse(record.getMessage().contains("SECRET"));
                assertTrue(record.getMessage().contains("DYNAMIC_SEARCH_UNKNOWN_FIELD"));
                count.incrementAndGet();
            }
            public void flush() {}
            public void close() {}
        };
        logger.addHandler(handler);
        try {
            LocalDynamicSearch.normalize("{\"filter\":{\"removed\":\"SECRET\"}}", "School", MODELS, null);
            assertEquals(1, count.get());
        } finally { logger.removeHandler(handler); }
    }
}
