package com.teaql.purchaseordernumbering;

import com.teaql.purchaseordernumbering.platform.Platform;
import com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder;
import io.teaql.core.RequestIntentException;
import io.teaql.core.UserContext;

final class PurchaseOrderNumbering {
    private static final String REJECTED_SUPPLIER = "Unaudited Supplier";

    private PurchaseOrderNumbering() {}

    static void verify(UserContext context, String round) {
        require(round != null && round.matches("[a-z]+"), "Supply the example round label");
        Platform platform = Q.platformsWithMinimalFields().withIdIs(1L).limit(1)
                .comment("Read the bootstrapped procurement platform")
                .purpose("Associate purchase orders with the procurement platform")
                .executeForOne(context);
        require(platform != null, "Procurement platform was not bootstrapped");

        PurchaseOrder first = create(context, platform, "Northwind-" + round);
        PurchaseOrder second = create(context, platform, "Contoso-" + round);
        Long firstId = first.getId();
        var firstNumber = first.getOrderNumber();
        var secondNumber = second.getOrderNumber();
        String format = "ORD-20261001-[A-Z0-9]{6}";
        require(firstNumber != null && secondNumber != null
                && !firstId.equals(second.getId()) && !firstNumber.equals(secondNumber)
                && firstNumber.value().matches(format) && secondNumber.value().matches(format),
                "Expected distinct numbers from the Context business day");
        first.auditAs("Retry recording Northwind's purchase order").save(context);
        require(firstId.equals(first.getId()) && firstNumber.equals(first.getOrderNumber()),
                "Retry changed the purchase order identity");

        for (PurchaseOrder order : new PurchaseOrder[] { first, second }) {
            var read = Q.purchaseOrders().withOrderNumberIs(order.getOrderNumber()).limit(2)
                    .comment("Read a purchase order by its assigned number")
                    .purpose("Verify the procurement record and allocation").executeForOne(context);
            require(read != null && read.getId().equals(order.getId())
                    && read.getOrderNumber().equals(order.getOrderNumber())
                    && read.getSupplier().equals(order.getSupplier()),
                    "Typed order-number query returned a different purchase order");
        }

        var rejected = prepare(context, platform, REJECTED_SUPPLIER);
        String rejection;
        try {
            rejected.auditAs(" ").save(context);
            throw new IllegalStateException("Unaudited purchase order was accepted");
        } catch (RequestIntentException error) {
            require(error.getCode().equals("REQUEST_COMMENT_REQUIRED")
                    && error.getRequestKind().equals("mutation"), "Unexpected intent failure");
            rejection = error.getCode();
        }
        var absent = Q.purchaseOrders().withSupplierIs(REJECTED_SUPPLIER).limit(2)
                .comment("Look for the unaudited purchase order")
                .purpose("Prove the rejected operation stored no order").executeForOne(context);
        require(absent == null, "Unaudited purchase order persisted");
        System.out.printf("PURCHASE_ORDER_NUMBERING={\"round\":\"%s\",\"firstId\":%d,\"firstNumber\":\"%s\","
                        + "\"secondId\":%d,\"secondNumber\":\"%s\",\"retryNumber\":\"%s\","
                        + "\"rejection\":\"%s\",\"typedReadback\":true}%n",
                round, firstId, firstNumber.value(), second.getId(), secondNumber.value(),
                first.getOrderNumber().value(), rejection);
    }

    private static PurchaseOrder prepare(UserContext context, Platform platform, String supplier) {
        var order = Q.purchaseOrders().comment("Initialize the purchase order")
                .purpose("Record a procurement commitment").newEntity(context);
        order.updatePlatform(platform);
        order.updateSupplier(supplier);
        return order;
    }

    private static PurchaseOrder create(UserContext context, Platform platform, String supplier) {
        var order = prepare(context, platform, supplier);
        order.auditAs("Record the approved purchase order").save(context);
        return order;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
