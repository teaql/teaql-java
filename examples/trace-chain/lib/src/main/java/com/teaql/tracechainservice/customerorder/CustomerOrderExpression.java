
package com.teaql.tracechainservice.customerorder;

import com.teaql.tracechainservice.orderitem.OrderItem;
import com.teaql.tracechainservice.orderitem.OrderItemListExpression;
import com.teaql.tracechainservice.payment.Payment;
import com.teaql.tracechainservice.payment.PaymentListExpression;
import com.teaql.tracechainservice.platform.Platform;
import com.teaql.tracechainservice.platform.PlatformExpression;
import com.teaql.tracechainservice.shipment.Shipment;
import com.teaql.tracechainservice.shipment.ShipmentListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class CustomerOrderExpression<T, E, U extends CustomerOrder> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public CustomerOrderExpression(Expression<T, U> expression){
        super(expression);
    }

    public CustomerOrderExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public CustomerOrderExpression<T, U, U> updateId(Long id){
        return new CustomerOrderExpression(this, $it -> {((CustomerOrder)$it).__internalSet("id", id); return this;});
     }

     public CustomerOrderExpression<T, U, U> save(UserContext userContext){
        return new CustomerOrderExpression(this, $it -> ((CustomerOrder)$it).auditAs("Saved by Expression").save(userContext));
     }

     public CustomerOrderExpression<T, U, U> save(String intent, UserContext userContext){
        return new CustomerOrderExpression(this, $it -> ((CustomerOrder)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public PlatformExpression<T, U, Platform> getPlatform(){
       return new PlatformExpression(loaded("platform", CustomerOrder::getPlatform));
    }

    public CustomerOrderExpression<T, U, U> updatePlatform(Platform platform){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updatePlatform(platform));
    }

    public Expression<T, String> getOrderNumber(){
       return loaded("orderNumber", CustomerOrder::getOrderNumber);
    }
    public CustomerOrderExpression<T, U, U> updateOrderNumber(String orderNumber){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateOrderNumber(orderNumber));
    }

    public Expression<T, String> getDescription(){
       return loaded("description", CustomerOrder::getDescription);
    }
    public CustomerOrderExpression<T, U, U> updateDescription(String description){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateDescription(description));
    }

    public OrderItemListExpression<T, U, OrderItem> getOrderItemList(){
        return new OrderItemListExpression(loaded("orderItemList", CustomerOrder::getOrderItemList));
    }
    public PaymentListExpression<T, U, Payment> getPaymentList(){
        return new PaymentListExpression(loaded("paymentList", CustomerOrder::getPaymentList));
    }
    public ShipmentListExpression<T, U, Shipment> getShipmentList(){
        return new ShipmentListExpression(loaded("shipmentList", CustomerOrder::getShipmentList));
    }
    public CustomerOrderExpression<T, U, U> addOrderItem(OrderItem orderItem){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).addOrderItem(orderItem));
    }
    public CustomerOrderExpression<T, U, U> addPayment(Payment payment){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).addPayment(payment));
    }
    public CustomerOrderExpression<T, U, U> addShipment(Shipment shipment){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).addShipment(shipment));
    }
}