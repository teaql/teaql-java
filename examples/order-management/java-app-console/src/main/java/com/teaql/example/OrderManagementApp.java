package com.teaql.example;

import com.teaql.ordermanagementservice.EntityMetaRegistry;
import com.teaql.ordermanagementservice.Q;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset;
import io.teaql.core.DataServiceExecutor;
import io.teaql.core.InternalIdGenerationService;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class OrderManagementApp {
    private static final String PURPOSE = "Operate the local order-management quick start";

    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        Path database = root.resolve(".local/order.db");
        if (Files.notExists(database)) {
            System.out.println("[database] " + database + " was not found; TeaQL will create it");
        }
        Files.createDirectories(database.getParent());
        DriverManagerDataSource source = new DriverManagerDataSource("jdbc:sqlite:" + database);
        JdbcSqlExecutor jdbc = new JdbcSqlExecutor(source);
        SqliteDataServiceExecutor executor = new SqliteDataServiceExecutor("sqlite", jdbc, source);
        UserContext ctx = context(executor);
        executor.ensureSchema(ctx);
        System.out.println("[schema] ensured 7 generated entity tables");

        SmartList<CommercePlatform> platforms = Q.commercePlatforms()
                .withNameIs("Northwind Demo")
                .comment("Check whether deterministic quick-start data exists")
                .purpose("Initialize the local order-management example")
                .executeForList(ctx);
        CommercePlatform platform;
        if (platforms.isEmpty()) {
            platform = new CommercePlatform().updateName("Northwind Demo");
            platform.auditAs("Create quick-start commerce platform").save(ctx);
        } else platform = platforms.get(0);

        SmartList<CustomerOrder> seeded = Q.customerOrders()
                .withOrderNumberIs("WEB-2026-001")
                .comment("Check whether the deterministic order exists")
                .purpose("Initialize the local order-management example")
                .executeForList(ctx);
        if (seeded.isEmpty()) {
            Customer customer = new Customer().updateName("Acme Retail")
                    .updateEmail("masked-in-quick-start").updateCommercePlatform(platform);
            customer.auditAs("Create masked quick-start customer").save(ctx);
            CustomerOrder order = new CustomerOrder().updateOrderNumber("WEB-2026-001")
                    .updateOrderDate(LocalDate.of(2026, 8, 12))
                    .updateTotalAmount(new BigDecimal("129.95"))
                    .updateStatusToPending().updateCustomer(customer).updateCommercePlatform(platform);
            order.auditAs("Create deterministic quick-start order").save(ctx);
            System.out.println("[seed] inserted deterministic customer and order");
        } else System.out.println("[seed] deterministic data already exists; no duplicate rows added");

        SmartList<CustomerOrder> orders = Q.customerOrders()
                .withOrderNumberContaining("WEB-").orderByIdAscending()
                .comment("List WEB orders for the terminal quick start")
                .purpose("Show the operator a deterministic order list")
                .executeForList(ctx);
        System.out.println("[query] matched " + orders.size() + " order(s)");
        for (CustomerOrder order : orders) {
            System.out.println("  " + order.getOrderNumber() + "  " + order.getOrderDate() + "  " + order.getTotalAmount());
        }

        SmartList<OrderSearchPreset> presets = Q.orderSearchPresets()
                .withRequestIdIs("quick-start-pending-orders")
                .comment("Check idempotent quick-start preset")
                .purpose("Persist the operator's reusable search")
                .executeForList(ctx);
        if (presets.isEmpty()) {
            OrderSearchPreset preset = new OrderSearchPreset().updateName("Pending web orders")
                    .updateFilterJson("{\"order_number\":\"WEB-\"}")
                    .updateRequestId("quick-start-pending-orders").updateOwnerUserId("quick-start-user")
                    .updateCommercePlatform(platform);
            preset.auditAs("Save idempotent quick-start search preset").save(ctx);
            System.out.println("[mutation] saved preset #" + preset.getId());
        } else System.out.println("[mutation] preset #" + presets.get(0).getId() + " already exists");
    }

    private static UserContext context(DataServiceExecutor executor) {
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        new EntityMetaRegistry().assemble(metadata);
        EntityMetaFactory.registerGlobal(metadata);
        AtomicLong ids = new AtomicLong(1000);
        InternalIdGenerationService idGeneration = (ctx, entity) -> ids.getAndIncrement();
        TeaQLRuntime runtime = TeaQLRuntime.builder().metadata(metadata)
                .dataService("default", executor).dataService("sqlite", executor)
                .idGenerationService(idGeneration).build();
        return new DefaultUserContext(runtime);
    }

    private record DriverManagerDataSource(String url) implements DataSource {
        public Connection getConnection() throws SQLException { return DriverManager.getConnection(url); }
        public Connection getConnection(String user, String password) throws SQLException { return getConnection(); }
        public PrintWriter getLogWriter() throws SQLException { return DriverManager.getLogWriter(); }
        public void setLogWriter(PrintWriter out) throws SQLException { DriverManager.setLogWriter(out); }
        public void setLoginTimeout(int seconds) throws SQLException { DriverManager.setLoginTimeout(seconds); }
        public int getLoginTimeout() throws SQLException { return DriverManager.getLoginTimeout(); }
        public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("unwrap unsupported"); }
        public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
