package com.teaql.example;

import com.teaql.ordermanagementservice.EntityMetaRegistry;
import com.teaql.ordermanagementservice.Q;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import io.teaql.core.InternalIdGenerationService;
import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.ContextBoundDocumentService.AcceptRequest;
import io.teaql.core.reference.ContextBoundDocumentService.ContextualDocument;
import io.teaql.core.reference.ContextBoundDocumentService.SubmittedDocument;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.core.view.AcceptedDocumentView;
import io.teaql.core.view.DocumentView;
import io.teaql.core.view.PreparedView;
import io.teaql.core.view.ViewAuthorization;
import io.teaql.core.view.ViewOptions;
import io.teaql.core.view.ViewPreparationException;
import io.teaql.core.view.ViewPreparationService;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.secure.document.reference.AeadRoundTripReferenceProvider;
import io.teaql.secure.document.reference.DeploymentProfile;
import io.teaql.secure.document.reference.RoundTripReferenceKey;
import io.teaql.secure.document.reference.SecureDocumentReferenceModule;
import io.teaql.secure.document.reference.StaticRoundTripReferenceKeyProvider;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class GeneratedOrderDocumentServiceTest {
    private static final String NUMBER = "GEN-DOC-1001";
    private Path database;
    private JdbcSqlExecutor jdbc;
    private TeaQLRuntime runtime;
    private UserContext context;
    private GeneratedOrderDocumentService service;
    private long orderId;

    private record LocalDataSource(String url) implements DataSource {
        @Override public Connection getConnection() throws SQLException { return DriverManager.getConnection(url); }
        @Override public Connection getConnection(String user, String password) throws SQLException { return getConnection(); }
        @Override public PrintWriter getLogWriter() throws SQLException { return DriverManager.getLogWriter(); }
        @Override public void setLogWriter(PrintWriter out) throws SQLException { DriverManager.setLogWriter(out); }
        @Override public void setLoginTimeout(int seconds) throws SQLException { DriverManager.setLoginTimeout(seconds); }
        @Override public int getLoginTimeout() throws SQLException { return DriverManager.getLoginTimeout(); }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        @Override public <T> T unwrap(Class<T> type) throws SQLException { throw new SQLException("unsupported"); }
        @Override public boolean isWrapperFor(Class<?> type) { return false; }
    }

    private void setup() throws Exception {
        database = Files.createTempFile("teaql-generated-document-", ".sqlite");
        LocalDataSource dataSource = new LocalDataSource("jdbc:sqlite:" + database);
        jdbc = new JdbcSqlExecutor(dataSource);
        SqliteDataServiceExecutor executor = new SqliteDataServiceExecutor("sqlite", jdbc, dataSource);
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        new EntityMetaRegistry().assemble(metadata);
        AtomicLong ids = new AtomicLong(1000);
        InternalIdGenerationService idGeneration = (ctx, entity) -> ids.getAndIncrement();
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 0x47);
        AeadRoundTripReferenceProvider provider = AeadRoundTripReferenceProvider.governed(
                DeploymentProfile.DEVELOPMENT, "generated-order-service", "sqlite-generated",
                new StaticRoundTripReferenceKeyProvider(new RoundTripReferenceKey("k1", key)),
                (ctx, principal, scope, identity) -> {
                    if (!"Customer".equals(principal.domainRootType())
                            || !"CustomerOrder".equals(scope.aggregateType())
                            || !"alice".equals(principal.subject())
                            || !allowed(principal)) {
                        throw new RoundTripReferenceException(
                                RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                                "AUTHORIZATION_REQUIRED");
                    }
                });
        runtime = SecureDocumentReferenceModule.of(provider)
                .installInto(TeaQLRuntime.builder().metadata(metadata)
                        .dataService("default", executor).dataService("sqlite", executor)
                        .idGenerationService(idGeneration)
                        .viewAuthorization(new ViewAuthorization() {
                            @Override public Identity identity(UserContext ctx) {
                                TrustedReferencePrincipal principal = ctx.trustedReferencePrincipal();
                                return new Identity(principal.authenticationRealm(), principal.subject(),
                                        principal.domainRootType() + ":" + principal.domainRootId());
                            }
                            @Override public boolean canReadEntity(
                                    UserContext ctx, EntityTarget entity, String path) { return false; }
                            @Override public FieldDecision field(UserContext ctx, EntityTarget entity,
                                    String field, String path, ViewOptions.Mode mode) {
                                return FieldDecision.hide();
                            }
                        })).build();
        context = new DefaultUserContext(runtime);
        context.ensureSchema();
        service = new GeneratedOrderDocumentService(jdbc);
        service.ensureDocumentTables();
        CommercePlatform platform = new CommercePlatform().updateName("Generated document platform");
        platform.auditAs("seed generated document platform").save(context);
        Customer customer = new Customer().updateName("Generated document customer")
                .updateEmail("customer@example.invalid").updateCommercePlatform(platform);
        customer.auditAs("seed generated document customer").save(context);
        CustomerOrder order = new CustomerOrder().updateOrderNumber(NUMBER)
                .updateOrderDate(LocalDate.of(2026, 10, 26))
                .updateTotalAmount(new BigDecimal("25.00"))
                .updateStatusToPending().updateCustomer(customer).updateCommercePlatform(platform);
        order.auditAs("seed generated document Order").save(context);
        orderId = order.getId();
        new OrderLine().updateCustomerOrder(order).updateProductName("Blue cup")
                .updateSku("CUP-1").updateQuantity(2).updateCommercePlatform(platform)
                .auditAs("seed generated item one").save(context);
        new OrderLine().updateCustomerOrder(order).updateProductName("Red cup")
                .updateSku("CUP-2").updateQuantity(3).updateCommercePlatform(platform)
                .auditAs("seed generated item two").save(context);
        jdbc.update("INSERT INTO generated_document_permissions(subject,customer_id,can_edit) "
                + "VALUES(?,?,1)", new Object[] {"alice", customer.getId()});
        context.withTrustedReferencePrincipal(new TrustedReferencePrincipal(
                "oidc", "alice", "Customer", customer.getId()));
        context.withContextBoundDocumentService(service);
    }

    private boolean allowed(TrustedReferencePrincipal principal) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT can_edit FROM generated_document_permissions WHERE subject=? AND customer_id=?",
                new Object[] {principal.subject(), principal.domainRootId()});
        return rows.size() == 1 && ((Number) rows.get(0).get("can_edit")).intValue() == 1;
    }

    @After public void cleanup() throws Exception {
        if (runtime != null) runtime.close();
        if (database != null) Files.deleteIfExists(database);
    }

    private CustomerOrder load() {
        var lines = Q.orderLines().orderByIdAscending();
        lines.top(101);
        var request = Q.customerOrders().withOrderNumberIs(NUMBER).selectOrderLineListWith(lines);
        request.top(2);
        return request.comment("Verify generated document persistence")
                .purpose("Assert current generated Order rows").executeForOne(context);
    }

    private static ViewOptions options(ViewOptions.BusinessCompletion completion) {
        return new ViewOptions(ViewOptions.Mode.GOVERNED, completion,
                null, Duration.ofMinutes(15), false);
    }

    private PreparedView open() {
        DocumentView source = context.openDocumentForView(
                new io.teaql.core.reference.ContextBoundDocumentService.OpenRequest(
                        "CustomerOrder", NUMBER, GeneratedOrderDocumentService.PURPOSE,
                        Duration.ofMinutes(15)));
        return context.capability(ViewPreparationService.class).prepare(
                context, source, options(ViewOptions.BusinessCompletion.QUERY));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> edit(PreparedView view, int quantity, boolean removeSecond) {
        Map<String, Object> original = (Map<String, Object>) view.value();
        List<Map<String, Object>> rows = (List<Map<String, Object>>) original.get("items");
        List<Map<String, Object>> changed = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            if (index == 1 && removeSecond) continue;
            Map<String, Object> row = new LinkedHashMap<>(rows.get(index));
            if (index == 0) row.put("quantity", quantity);
            changed.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNumber", NUMBER);
        result.put("items", changed);
        if (removeSecond) result.put("removedRefs", List.of("item:1"));
        return result;
    }

    private static AcceptRequest accept(PreparedView view, String command, Map<String, Object> body) {
        Map<String, String> credentials = view.documentCredentials();
        return new AcceptRequest("CustomerOrder", GeneratedOrderDocumentService.PURPOSE,
                new SubmittedDocument(credentials.get("businessId"),
                        credentials.get("documentToken"), command, body));
    }

    private static void fails(RoundTripReferenceErrorCode code, Runnable action) {
        RoundTripReferenceException failure = Assert.assertThrows(
                RoundTripReferenceException.class, action::run);
        Assert.assertEquals(code, failure.getCode());
    }

    private long scalar(String sql) {
        return ((Number) jdbc.queryForList(sql, new Object[0]).get(0).values().iterator().next()).longValue();
    }

    @Test public void generatedEntitiesAcceptUpdateRemovalAndIdempotentReplay() throws Exception {
        setup();
        PreparedView opened = open();
        Assert.assertTrue(opened.documentCredentials().get("documentToken").startsWith("tqd1.k1."));
        AcceptRequest request = accept(opened, "generated-command-1", edit(opened, 5, true));
        AcceptedDocumentView accepted = context.acceptDocumentForView(request);
        PreparedView returned = context.capability(ViewPreparationService.class).prepare(
                context, accepted.document(), options(ViewOptions.BusinessCompletion.COMMITTED));
        Assert.assertEquals(accepted.newRevision(), load().getVersion().longValue());
        Assert.assertEquals(1, load().getOrderLineList().size());
        Assert.assertEquals(Integer.valueOf(5), load().getOrderLineList().get(0).getQuantity());
        Assert.assertEquals(2, accepted.changedEntities().size());
        Assert.assertEquals(load().getOrderLineList().get(0).getVersion().longValue(),
                accepted.changedEntities().get(0).version());
        Assert.assertNotEquals(opened.documentCredentials().get("documentToken"),
                returned.documentCredentials().get("documentToken"));
        AcceptedDocumentView replay = context.acceptDocumentForView(request);
        Assert.assertEquals(accepted.newRevision(), replay.newRevision());
        Assert.assertEquals(1, scalar("SELECT count(*) AS count FROM generated_document_commands"));
        fails(RoundTripReferenceErrorCode.DOCUMENT_COMMAND_CONFLICT,
                () -> context.acceptDocument(accept(opened, "generated-command-1", edit(opened, 6, true))));
    }

    @Test public void generatedProjectionAndRevisionChecksRejectWithoutSaving() throws Exception {
        setup();
        PreparedView opened = open();
        Map<String, Object> forged = edit(opened, 5, false);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) forged.get("items");
        rows.get(0).put("productName", "forged");
        fails(RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                () -> context.acceptDocument(accept(opened, "forged", forged)));
        Map<String, Object> missing = edit(opened, 5, false);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> missingRows = (List<Map<String, Object>>) missing.get("items");
        missingRows.remove(1);
        fails(RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                () -> context.acceptDocument(accept(opened, "missing-item", missing)));
        Assert.assertEquals(2, load().getOrderLineList().get(0).getQuantity().intValue());
        CustomerOrder concurrent = load();
        concurrent.updateUpdateTime(context.businessTime())
                .auditAs("simulate concurrent generated Order update").save(context);
        fails(RoundTripReferenceErrorCode.DOCUMENT_REVISION_CONFLICT,
                () -> context.acceptDocument(accept(opened, "stale", edit(opened, 5, false))));
        Assert.assertEquals(0, scalar("SELECT count(*) AS count FROM generated_document_commands"));
    }

    @Test public void generatedSaveFailureRollsBackOrderAndReceiptThenCanRetry() throws Exception {
        setup();
        PreparedView opened = open();
        long originalVersion = load().getVersion();
        List<Map<String, Object>> tables = jdbc.queryForList(
                "SELECT name FROM sqlite_master WHERE type='table' "
                        + "AND lower(name) LIKE '%order%line%'", new Object[0]);
        Assert.assertEquals(1, tables.size());
        String table = (String) tables.get(0).get("name");
        Assert.assertTrue(table.matches("[A-Za-z_][A-Za-z0-9_]*"));
        jdbc.execute("CREATE TRIGGER reject_generated_item BEFORE UPDATE ON " + table
                + " BEGIN SELECT RAISE(ABORT,'generated item write failed'); END");
        AcceptRequest request = accept(opened, "retry-generated", edit(opened, 5, false));
        Assert.assertThrows(RuntimeException.class, () -> context.acceptDocument(request));
        Assert.assertEquals(originalVersion, load().getVersion().longValue());
        Assert.assertEquals(2, load().getOrderLineList().get(0).getQuantity().intValue());
        Assert.assertEquals(0, scalar("SELECT count(*) AS count FROM generated_document_commands"));
        jdbc.execute("DROP TRIGGER reject_generated_item");
        Assert.assertTrue(context.acceptDocument(request).newRevision() > originalVersion);
    }

    @Test public void postCommitGeneratedPresentationFailurePreservesOneMutation() throws Exception {
        setup();
        PreparedView opened = open();
        jdbc.execute("CREATE TRIGGER revoke_generated_document AFTER INSERT ON generated_document_commands "
                + "BEGIN UPDATE generated_document_permissions SET can_edit=0 WHERE subject='alice'; END");
        ViewPreparationException failed = Assert.assertThrows(ViewPreparationException.class,
                () -> context.acceptDocumentForView(
                        accept(opened, "committed-generated", edit(opened, 5, false))));
        Assert.assertTrue(failed.businessCommitted());
        Assert.assertEquals(5, load().getOrderLineList().get(0).getQuantity().intValue());
        Assert.assertEquals(1, scalar("SELECT count(*) AS count FROM generated_document_commands"));
    }

    @Test public void receiptInsertFailureRollsBackGeneratedSavesOnSamePhysicalOwner() throws Exception {
        setup();
        PreparedView opened = open();
        long originalVersion = load().getVersion();
        jdbc.execute("CREATE TRIGGER reject_generated_receipt BEFORE INSERT ON generated_document_commands "
                + "BEGIN SELECT RAISE(ABORT,'generated receipt write failed'); END");
        AcceptRequest request = accept(opened, "receipt-retry", edit(opened, 5, false));
        Assert.assertThrows(RuntimeException.class, () -> context.acceptDocument(request));
        Assert.assertEquals(originalVersion, load().getVersion().longValue());
        Assert.assertEquals(2, load().getOrderLineList().get(0).getQuantity().intValue());
        Assert.assertEquals(0, scalar("SELECT count(*) AS count FROM generated_document_commands"));
        jdbc.execute("DROP TRIGGER reject_generated_receipt");
        Assert.assertTrue(context.acceptDocument(request).newRevision() > originalVersion);
        Assert.assertEquals(1, scalar("SELECT count(*) AS count FROM generated_document_commands"));
    }
}
