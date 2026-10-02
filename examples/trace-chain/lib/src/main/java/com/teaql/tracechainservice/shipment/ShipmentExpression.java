
package com.teaql.tracechainservice.shipment;

import com.teaql.tracechainservice.customerorder.CustomerOrder;
import com.teaql.tracechainservice.customerorder.CustomerOrderExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class ShipmentExpression<T, E, U extends Shipment> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public ShipmentExpression(Expression<T, U> expression){
        super(expression);
    }

    public ShipmentExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public ShipmentExpression<T, U, U> updateId(Long id){
        return new ShipmentExpression(this, $it -> {((Shipment)$it).__internalSet("id", id); return this;});
     }

     public ShipmentExpression<T, U, U> save(UserContext userContext){
        return new ShipmentExpression(this, $it -> ((Shipment)$it).auditAs("Saved by Expression").save(userContext));
     }

     public ShipmentExpression<T, U, U> save(String intent, UserContext userContext){
        return new ShipmentExpression(this, $it -> ((Shipment)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public CustomerOrderExpression<T, U, CustomerOrder> getCustomerOrder(){
       return new CustomerOrderExpression(loaded("customerOrder", Shipment::getCustomerOrder));
    }

    public ShipmentExpression<T, U, U> updateCustomerOrder(CustomerOrder customerOrder){
       return new ShipmentExpression(this, $it ->  ((Shipment)$it).updateCustomerOrder(customerOrder));
    }

    public Expression<T, String> getReferenceCode(){
       return loaded("referenceCode", Shipment::getReferenceCode);
    }
    public ShipmentExpression<T, U, U> updateReferenceCode(String referenceCode){
       return new ShipmentExpression(this, $it ->  ((Shipment)$it).updateReferenceCode(referenceCode));
    }

}