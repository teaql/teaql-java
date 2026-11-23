
package com.teaql.purchaseordernumbering.purchaseorder;

import com.teaql.purchaseordernumbering.platform.Platform;
import com.teaql.purchaseordernumbering.platform.PlatformExpression;
import com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.OrderNumber;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class PurchaseOrderExpression<T, E, U extends PurchaseOrder> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public PurchaseOrderExpression(Expression<T, U> expression){
        super(expression);
    }

    public PurchaseOrderExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public PurchaseOrderExpression<T, U, U> updateId(Long id){
        return new PurchaseOrderExpression(this, $it -> {((PurchaseOrder)$it).__internalSet("id", id); return this;});
     }

     public PurchaseOrderExpression<T, U, U> save(UserContext userContext){
        return new PurchaseOrderExpression(this, $it -> ((PurchaseOrder)$it).auditAs("Saved by Expression").save(userContext));
     }

     public PurchaseOrderExpression<T, U, U> save(String intent, UserContext userContext){
        return new PurchaseOrderExpression(this, $it -> ((PurchaseOrder)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public PlatformExpression<T, U, Platform> getPlatform(){
       return new PlatformExpression(loaded("platform", "getPlatform", PurchaseOrder::getPlatform));
    }

    public PurchaseOrderExpression<T, U, U> updatePlatform(Platform platform){
       return new PurchaseOrderExpression(this, $it ->  ((PurchaseOrder)$it).updatePlatform(platform));
    }

    public Expression<T, String> getSupplier(){
       return loaded("supplier", "getSupplier", PurchaseOrder::getSupplier);
    }
    public PurchaseOrderExpression<T, U, U> updateSupplier(String supplier){
       return new PurchaseOrderExpression(this, $it ->  ((PurchaseOrder)$it).updateSupplier(supplier));
    }

    public Expression<T, OrderNumber> getOrderNumber(){
       return loaded("order_number", "getOrderNumber", PurchaseOrder::getOrderNumber);
    }
    public PurchaseOrderExpression<T, U, U> updateOrderNumber(OrderNumber orderNumber){
       return new PurchaseOrderExpression(this, $it ->  ((PurchaseOrder)$it).updateOrderNumber(orderNumber));
    }

}