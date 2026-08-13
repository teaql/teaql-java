
package com.teaql.ordermanagementservice.commerceplatform;

import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.customer.CustomerListExpression;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderListExpression;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderline.OrderLineListExpression;
import com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset;
import com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPresetListExpression;
import com.teaql.ordermanagementservice.orderstatus.OrderStatus;
import com.teaql.ordermanagementservice.orderstatus.OrderStatusListExpression;
import com.teaql.ordermanagementservice.product.Product;
import com.teaql.ordermanagementservice.product.ProductListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.time.LocalDateTime;
import java.util.function.Function;

public class CommercePlatformExpression<T, E, U extends CommercePlatform> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public CommercePlatformExpression(Expression<T, U> expression){
        super(expression);
    }

    public CommercePlatformExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public CommercePlatformExpression<T, U, U> updateId(Long id){
        return new CommercePlatformExpression(this, $it -> {((CommercePlatform)$it).__internalSet("id", id); return this;});
     }

     public CommercePlatformExpression<T, U, U> save(UserContext userContext){
        return new CommercePlatformExpression(this, $it -> ((CommercePlatform)$it).auditAs("Saved by Expression").save(userContext));
     }

     public CommercePlatformExpression<T, U, U> save(String intent, UserContext userContext){
        return new CommercePlatformExpression(this, $it -> ((CommercePlatform)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return apply(CommercePlatform::getName);
    }
    public CommercePlatformExpression<T, U, U> updateName(String name){
       return new CommercePlatformExpression(this, $it ->  ((CommercePlatform)$it).updateName(name));
    }

    public Expression<T, LocalDateTime> getCreateTime(){
       return apply(CommercePlatform::getCreateTime);
    }
    public CommercePlatformExpression<T, U, U> updateCreateTime(LocalDateTime createTime){
       return new CommercePlatformExpression(this, $it ->  ((CommercePlatform)$it).updateCreateTime(createTime));
    }

    public Expression<T, LocalDateTime> getUpdateTime(){
       return apply(CommercePlatform::getUpdateTime);
    }
    public CommercePlatformExpression<T, U, U> updateUpdateTime(LocalDateTime updateTime){
       return new CommercePlatformExpression(this, $it ->  ((CommercePlatform)$it).updateUpdateTime(updateTime));
    }

    public CustomerListExpression<T, U, Customer> getCustomerList(){
        return new CustomerListExpression(this, $it ->  ((CommercePlatform)$it).getCustomerList());
    }
    public OrderStatusListExpression<T, U, OrderStatus> getOrderStatusList(){
        return new OrderStatusListExpression(this, $it ->  ((CommercePlatform)$it).getOrderStatusList());
    }
    public CustomerOrderListExpression<T, U, CustomerOrder> getCustomerOrderList(){
        return new CustomerOrderListExpression(this, $it ->  ((CommercePlatform)$it).getCustomerOrderList());
    }
    public ProductListExpression<T, U, Product> getProductList(){
        return new ProductListExpression(this, $it ->  ((CommercePlatform)$it).getProductList());
    }
    public OrderLineListExpression<T, U, OrderLine> getOrderLineList(){
        return new OrderLineListExpression(this, $it ->  ((CommercePlatform)$it).getOrderLineList());
    }
    public OrderSearchPresetListExpression<T, U, OrderSearchPreset> getOrderSearchPresetList(){
        return new OrderSearchPresetListExpression(this, $it ->  ((CommercePlatform)$it).getOrderSearchPresetList());
    }
    public CommercePlatformExpression<T, U, U> addCustomer(Customer customer){
       return new CommercePlatformExpression(this, $it ->  ((CommercePlatform)$it).addCustomer(customer));
    }
    public CommercePlatformExpression<T, U, U> addOrderStatus(OrderStatus orderStatus){
       return new CommercePlatformExpression(this, $it ->  ((CommercePlatform)$it).addOrderStatus(orderStatus));
    }
    public CommercePlatformExpression<T, U, U> addCustomerOrder(CustomerOrder customerOrder){
       return new CommercePlatformExpression(this, $it ->  ((CommercePlatform)$it).addCustomerOrder(customerOrder));
    }
    public CommercePlatformExpression<T, U, U> addProduct(Product product){
       return new CommercePlatformExpression(this, $it ->  ((CommercePlatform)$it).addProduct(product));
    }
    public CommercePlatformExpression<T, U, U> addOrderLine(OrderLine orderLine){
       return new CommercePlatformExpression(this, $it ->  ((CommercePlatform)$it).addOrderLine(orderLine));
    }
    public CommercePlatformExpression<T, U, U> addOrderSearchPreset(OrderSearchPreset orderSearchPreset){
       return new CommercePlatformExpression(this, $it ->  ((CommercePlatform)$it).addOrderSearchPreset(orderSearchPreset));
    }
}