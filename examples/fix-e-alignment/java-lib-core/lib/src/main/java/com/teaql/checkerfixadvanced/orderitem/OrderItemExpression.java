
package com.teaql.checkerfixadvanced.orderitem;

import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrderExpression;
import com.teaql.checkerfixadvanced.tenant.Tenant;
import com.teaql.checkerfixadvanced.tenant.TenantExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Function;

public class OrderItemExpression<T, E, U extends OrderItem> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public OrderItemExpression(Expression<T, U> expression){
        super(expression);
    }

    public OrderItemExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public OrderItemExpression<T, U, U> updateId(Long id){
        return new OrderItemExpression(this, $it -> {((OrderItem)$it).__internalSet("id", id); return this;});
     }

     public OrderItemExpression<T, U, U> save(UserContext userContext){
        return new OrderItemExpression(this, $it -> ((OrderItem)$it).auditAs("Saved by Expression").save(userContext));
     }

     public OrderItemExpression<T, U, U> save(String intent, UserContext userContext){
        return new OrderItemExpression(this, $it -> ((OrderItem)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public PurchaseOrderExpression<T, U, PurchaseOrder> getPurchaseOrder(){
       return new PurchaseOrderExpression(loaded("purchase_order", "getPurchaseOrder", OrderItem::getPurchaseOrder));
    }

    public OrderItemExpression<T, U, U> updatePurchaseOrder(PurchaseOrder purchaseOrder){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updatePurchaseOrder(purchaseOrder));
    }

    public TenantExpression<T, U, Tenant> getTenant(){
       return new TenantExpression(loaded("tenant", "getTenant", OrderItem::getTenant));
    }

    public OrderItemExpression<T, U, U> updateTenant(Tenant tenant){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateTenant(tenant));
    }

    public Expression<T, String> getProductName(){
       return loaded("product_name", "getProductName", OrderItem::getProductName);
    }
    public OrderItemExpression<T, U, U> updateProductName(String productName){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateProductName(productName));
    }

    public Expression<T, BigDecimal> getUnitPrice(){
       return loaded("unit_price", "getUnitPrice", OrderItem::getUnitPrice);
    }
    public OrderItemExpression<T, U, U> updateUnitPrice(BigDecimal unitPrice){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateUnitPrice(unitPrice));
    }

    public Expression<T, LocalDateTime> getCreateTime(){
       return loaded("create_time", "getCreateTime", OrderItem::getCreateTime);
    }
    public OrderItemExpression<T, U, U> updateCreateTime(LocalDateTime createTime){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateCreateTime(createTime));
    }

    public Expression<T, LocalDateTime> getUpdateTime(){
       return loaded("update_time", "getUpdateTime", OrderItem::getUpdateTime);
    }
    public OrderItemExpression<T, U, U> updateUpdateTime(LocalDateTime updateTime){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateUpdateTime(updateTime));
    }

}