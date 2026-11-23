
package com.teaql.checkerfixadvanced.purchaseorder;

import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.orderitem.OrderItemListExpression;
import com.teaql.checkerfixadvanced.tenant.Tenant;
import com.teaql.checkerfixadvanced.tenant.TenantExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.time.LocalDateTime;
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


    public TenantExpression<T, U, Tenant> getTenant(){
       return new TenantExpression(loaded("tenant", "getTenant", PurchaseOrder::getTenant));
    }

    public PurchaseOrderExpression<T, U, U> updateTenant(Tenant tenant){
       return new PurchaseOrderExpression(this, $it ->  ((PurchaseOrder)$it).updateTenant(tenant));
    }

    public Expression<T, String> getContactPhone(){
       return loaded("contact_phone", "getContactPhone", PurchaseOrder::getContactPhone);
    }
    public PurchaseOrderExpression<T, U, U> updateContactPhone(String contactPhone){
       return new PurchaseOrderExpression(this, $it ->  ((PurchaseOrder)$it).updateContactPhone(contactPhone));
    }

    public Expression<T, String> getInternalNote(){
       return loaded("internal_note", "getInternalNote", PurchaseOrder::getInternalNote);
    }
    public PurchaseOrderExpression<T, U, U> updateInternalNote(String internalNote){
       return new PurchaseOrderExpression(this, $it ->  ((PurchaseOrder)$it).updateInternalNote(internalNote));
    }

    public Expression<T, LocalDateTime> getCreateTime(){
       return loaded("create_time", "getCreateTime", PurchaseOrder::getCreateTime);
    }
    public PurchaseOrderExpression<T, U, U> updateCreateTime(LocalDateTime createTime){
       return new PurchaseOrderExpression(this, $it ->  ((PurchaseOrder)$it).updateCreateTime(createTime));
    }

    public Expression<T, LocalDateTime> getUpdateTime(){
       return loaded("update_time", "getUpdateTime", PurchaseOrder::getUpdateTime);
    }
    public PurchaseOrderExpression<T, U, U> updateUpdateTime(LocalDateTime updateTime){
       return new PurchaseOrderExpression(this, $it ->  ((PurchaseOrder)$it).updateUpdateTime(updateTime));
    }

    public OrderItemListExpression<T, U, OrderItem> getOrderItemList(){
        return new OrderItemListExpression(loaded("order_item_list", "getOrderItemList", PurchaseOrder::getOrderItemList));
    }
    public PurchaseOrderExpression<T, U, U> addOrderItem(OrderItem orderItem){
       return new PurchaseOrderExpression(this, $it ->  ((PurchaseOrder)$it).addOrderItem(orderItem));
    }
}