package com.teaql.ordereditreference;

import com.teaql.ordereditreference.orderitem.OrderItem;
import com.teaql.ordereditreference.salesorder.SalesOrder;
import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.runtime.reference.AeadRoundTripReferenceProvider;
import io.teaql.runtime.reference.DeploymentProfile;
import io.teaql.runtime.reference.RoundTripReferenceKey;
import io.teaql.runtime.reference.StaticRoundTripReferenceKeyProvider;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class OrderReferenceEditorTest {
    private static final String PURPOSE = "edit-order";
    private static final Duration REFERENCE_LIFETIME = Duration.ofMinutes(15);

    private static UserContext editor(TeaQLRuntime runtime, String subject, long merchant) {
        return new DefaultUserContext(runtime).withTrustedReferencePrincipal(
                new TrustedReferencePrincipal("oidc", subject, "Merchant", merchant));
    }

    private static ReferenceDocumentScope scope(SalesOrder order, String purpose, long orderId) {
        return new ReferenceDocumentScope("editor-" + order.getId(), purpose, "SalesOrder",
                orderId, order.getVersion());
    }

    private static OrderItem loadLine(UserContext context, long id) {
        return Q.orderItems().withIdIs(id).selectSalesOrder().limit(1)
                .comment("Read the line by its authenticated ID")
                .purpose("Verify current version and order membership before editing")
                .executeForOne(context);
    }

    private static void amend(UserContext context, SalesOrder order, Object token, int quantity) {
        var claims = context.resolveReference(token, scope(order, PURPOSE, order.getId()), "OrderItem");
        var line = loadLine(context, claims.id());
        if (line == null || !order.getId().equals(E.orderItem(line).getSalesOrder().eval().getId())) {
            throw new IllegalArgumentException("Line is outside the order being edited");
        }
        if (line.getVersion() != claims.version()) {
            throw new IllegalStateException("The edit page is stale");
        }
        line.updateQuantity(quantity);
        line.auditAs("Accept the edited tea quantity").save(context);
    }

    @Test
    public void authenticatedReferenceResolvesARealOrderLineAndCannotReplayAnEdit() throws Exception {
        var file = Path.of(System.getenv("TEAQL_REFERENCE_DATABASE"));
        var round = System.getenv("TEAQL_SHOWCASE_ROUND");
        var dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + file);
        var jdbc = new JdbcSqlExecutor(dataSource);
        var sqlite = new SqliteDataServiceExecutor("sqlite", jdbc, dataSource);
        var generator = new IdSpaceIdGenerator(new WorkspaceTeaQLDatabase(jdbc, dataSource));
        generator.ensureIdSpaceTable();
        var canEdit = new AtomicBoolean(true);
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 0x27);
        var codec = AeadRoundTripReferenceProvider.governed(DeploymentProfile.TEST, "order-editor",
                "local", new StaticRoundTripReferenceKeyProvider(new RoundTripReferenceKey("fixture", key)),
                (context, principal, scope, identity) -> {
                    if (!canEdit.get() || !"alice".equals(principal.subject())
                            || !"Merchant".equals(principal.domainRootType())
                            || principal.domainRootId() != 1 || !"SalesOrder".equals(scope.aggregateType())
                            || !PURPOSE.equals(scope.purpose()) || !"OrderItem".equals(identity.entityType())) {
                        throw new RoundTripReferenceException(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                                "The current merchant editor policy denies this reference");
                    }
                });
        var runtime = TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory()).dataService("default", sqlite).dataService("sqlite", sqlite)
                .idGenerationService(generator).roundTripReferenceProvider(codec)
                .build().install(GeneratedRuntimeModule.module());
        var context = editor(runtime, "alice", 1);
        context.ensureSchema();
        assertTrue(OrderItem.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()
                .endsWith("/java-lib-core/lib/target/classes/"));
        var merchant = Q.merchantsWithMinimalFields().withIdIs(1L).limit(1)
                .comment("Read the initialized merchant").purpose("Place the order under the trusted merchant")
                .executeForOne(context);
        assertNotNull(merchant);
        String orderNumber = "EDIT-042-" + round;
        var order = Q.salesOrders().comment("Prepare the order being edited")
                .purpose("Create the merchant's order").newEntity(context);
        order.updateMerchant(merchant);
        order.updateOrderNumber(orderNumber);
        order.auditAs("Open an editable order").save(context);
        var line = Q.orderItems().comment("Prepare one tea line")
                .purpose("Record a real initial quantity for the edit page").newEntity(context);
        line.updateSalesOrder(order);
        line.updateSku("TEA-001");
        line.updateQuantity(2);
        line.auditAs("Record the initial tea quantity").save(context);

        var stored = loadLine(context, line.getId());
        assertNotNull(stored);
        var scope = scope(order, PURPOSE, order.getId());
        String wire = (String) context.referenceFor(stored, scope, REFERENCE_LIFETIME);
        assertTrue(wire.startsWith("tqr1.fixture."));
        var claims = context.resolveReference(wire, scope, "OrderItem");
        assertEquals(stored.getId().longValue(), claims.id());
        assertEquals(1L, claims.version());

        int midpoint = wire.length() / 2;
        String tampered = wire.substring(0, midpoint) + (wire.charAt(midpoint) == 'A' ? "B" : "A")
                + wire.substring(midpoint + 1);
        assertThrows(RoundTripReferenceException.class, () -> context.resolveReference(tampered, scope, "OrderItem"));
        assertThrows(RoundTripReferenceException.class, () -> context.resolveReference(wire, scope(order, "view-order", order.getId()), "OrderItem"));
        assertThrows(RoundTripReferenceException.class, () -> context.resolveReference(wire, scope(order, PURPOSE, order.getId() + 1), "OrderItem"));
        assertThrows(RoundTripReferenceException.class, () -> editor(runtime, "bob", 1).resolveReference(wire, scope, "OrderItem"));
        assertThrows(RoundTripReferenceException.class, () -> editor(runtime, "alice", 2).resolveReference(wire, scope, "OrderItem"));
        canEdit.set(false);
        assertEquals(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                assertThrows(RoundTripReferenceException.class, () -> amend(context, order, wire, 99)).getCode());
        canEdit.set(true);
        amend(context, order, wire, 4);
        assertEquals("The edit page is stale",
                assertThrows(IllegalStateException.class, () -> amend(context, order, wire, 99)).getMessage());
        var updated = loadLine(context, line.getId());
        assertEquals(Integer.valueOf(4), E.orderItem(updated).getQuantity().eval());
        assertEquals(Long.valueOf(2), updated.getVersion());
        assertEquals("TEA-001", E.orderItem(updated).getSku().eval());
        assertEquals(order.getId(), E.orderItem(updated).getSalesOrder().eval().getId());
        System.out.printf("ORDER_REFERENCE round=%s order_id=%d line_id=%d order_number=%s quantity=4 version=2 rejections=7%n",
                round, order.getId(), line.getId(), orderNumber);
    }
}
