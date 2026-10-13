package com.example.requesttreeorderservice;

import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import com.example.requesttreeorderservice.orderstatus.OrderStatus;
import io.teaql.core.InternalIdGenerationService;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.query.json.JsonRequests;
import io.teaql.runtime.AppAuditEventSink;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.SafeAuditEvent;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Ordinary generated business code: no SDK setup, manual span or application SQL. */
public class StandardOrderTelemetryTest {
    private static final String PRIVATE = "STANDARD_PRIVATE_ORDER_79463A12";

    @Test public void completeOrderPagesKeepCountsFacetsRelationsAndAutomaticDiagnostics() throws Exception {
        var source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + Files.createTempDirectory("teaql-java-standard-order-").resolve("orders.sqlite"));
        var service = new SqliteDataServiceExecutor("sqlite", new JdbcSqlExecutor(source), source);
        var ids = new AtomicLong(2000);
        var ordinaryLogs = new AtomicInteger();
        var audits = new ArrayList<SafeAuditEvent>();
        try (var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .dataService("default", service).dataService("sqlite", service)
                .idGenerationService(new InternalIdGenerationService() {
                    @Override public Long generateId(UserContext context, io.teaql.core.Entity entity) { return ids.incrementAndGet(); }
                    @Override public void ensureFloor(String type, long floor) { ids.accumulateAndGet(floor, Math::max); }
                }).queryExecutionLogging(false).mutationExecutionLogging(false)
                .logSink((context, metadata) -> ordinaryLogs.incrementAndGet()).build()
                .install(GeneratedRuntimeModule.module())) {
            var context = new DefaultUserContext(runtime);
            context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> audits.add(event));
            context.ensureSchema();
            assertEquals(4, audits.size());
            context.ensureSchema();
            assertEquals("repeat seed must not create another audit", 4, audits.size());
            var platform = Q.platforms().limit(1).comment("load seeded commerce root")
                    .purpose("prepare the operational queue").executeForOne(context);
            var customer = Q.customers().comment("create fixture customer").purpose("prepare order graph").newEntity(context);
            customer.updateName("Controlled retail"); customer.updatePlatform(platform);
            customer.auditAs("seed operational queue customer").save(context);
            for (int index = 1; index <= 4; index++) {
                var order = Q.customerOrders().comment("create fixture order").purpose("prepare order graph").newEntity(context);
                order.updateCustomer(customer);
                order.updateOrderNumber(index < 4 ? PRIVATE + "-00" + index : "OTHER-004");
                order.updateOrderDate(LocalDate.of(2026, 10, index));
                order.updateTotalAmount(BigDecimal.valueOf(index < 4 ? index * 100 : 900));
                if (index == 1) order.updateStatusToNew(); else order.updateStatusToPaid();
                order.auditAs("seed operational queue order").save(context);
                for (int item = 1; item <= 5; item++) {
                    var line = Q.orderItems().comment("create fixture item").purpose("prepare bounded item previews").newEntity(context);
                    line.updateCustomerOrder(order); line.updateProductName("Box " + item);
                    line.updateQuantity(item); line.updateUnitPrice(BigDecimal.valueOf(20));
                    line.auditAs("seed operational queue item").save(context);
                }
            }
            int beforeQueries = audits.size();
            for (int repetition = 1; repetition <= 2; repetition++) verifyPage(fullOrderPage(context));
            assertEquals("queries must not create committed audit events", beforeQueries, audits.size());
            assertEquals("ordinary SQL output stays independently disabled", 0, ordinaryLogs.get());
            runtime.getTelemetry().flush();
        }
        System.out.println("JAVA_STANDARD_ORDER PASSED pages=2 total=3 rows=1 preview=3 itemCount=5 facets=3 seeds=4 repeatedSeedWrites=0 ordinarySqlLogs=0");
    }

    private static SmartList<CustomerOrder> fullOrderPage(UserContext context) {
        return JsonRequests.findWithJson(context, Q.customerOrdersWithMinimalFields(), "{\"orderNumber\":\"" + PRIVATE + "\"}")
                .countAs("totalOrders").facetByStatusAs("statusCounts", statusFacets())
                .selectOrderNumber().selectOrderDate().selectStatusWith(statusDetails())
                .selectOrderItemListWith(itemPreviews()).countOrderItemsAs("itemCount")
                .orderByOrderDateDescending().orderByIdDescending().offset(1, 1)
                .comment("load order list").purpose("render order list page").executeForList(context);
    }
    private static com.example.requesttreeorderservice.orderstatus.OrderStatusRequest statusDetails() {
        return Q.orderStatusesWithMinimalFields().selectName().selectColor().comment("load status");
    }
    private static com.example.requesttreeorderservice.orderitem.OrderItemRequest itemPreviews() {
        return Q.orderItemsWithMinimalFields().selectProductName().selectQuantity()
                .orderByIdAscending().limit(3).comment("load item previews");
    }
    private static com.example.requesttreeorderservice.orderstatus.OrderStatusRequest statusFacets() {
        return Q.orderStatusesWithMinimalFields().selectName().selectColor().countAs("orderCount")
                .sumTotalAmountOfCustomerOrdersAs("orderTotalAmount")
                .orderByDisplayOrderAscending().comment("load status facets");
    }
    private static void verifyPage(SmartList<CustomerOrder> rows) {
        assertEquals(1, rows.size()); assertEquals(3, rows.getTotalCount());
        assertEquals(3, ((Number) rows.aggregationNumberProperties().get("totalOrders")).intValue());
        var row = rows.first();
        assertEquals(PRIVATE + "-002", E.customerOrder(row).getOrderNumber().eval());
        assertEquals(LocalDate.of(2026, 10, 2), E.customerOrder(row).getOrderDate().eval());
        assertEquals("Paid", E.customerOrder(row).getStatus().getName().eval());
        assertEquals(Integer.valueOf(3), E.customerOrder(row).getOrderItemList().size().eval());
        assertEquals("Box 1", E.customerOrder(row).getOrderItemList().first().getProductName().eval());
        assertEquals(5, ((Number) row.getDynamicProperty("itemCount")).intValue());
        var facets = rows.getFacet("statusCounts"); assertEquals(3, facets.size());
        int[] counts = {1, 2, 0}, sums = {100, 500, 0};
        String[] names = {"New", "Paid", "Cancelled"};
        for (int index = 0; index < 3; index++) {
            var facet = (OrderStatus) facets.get(index);
            assertEquals(names[index], E.orderStatus(facet).getName().eval());
            assertEquals(counts[index], ((Number) facet.getDynamicProperty("orderCount")).intValue());
            assertEquals(0, new BigDecimal(facet.getDynamicProperty("orderTotalAmount").toString()).compareTo(BigDecimal.valueOf(sums[index])));
        }
        var unloaded = assertThrows(RuntimeException.class, () -> E.customerOrder(row).getTotalAmount().eval());
        assertTrue("an omitted amount must be NotLoaded, not null", unloaded.getClass().getSimpleName().contains("NotLoaded"));
    }
}
