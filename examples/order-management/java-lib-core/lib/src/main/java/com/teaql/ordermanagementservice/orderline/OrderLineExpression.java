
package com.teaql.ordermanagementservice.orderline;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformExpression;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderExpression;
import com.teaql.ordermanagementservice.product.Product;
import com.teaql.ordermanagementservice.product.ProductExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.time.LocalDateTime;
import java.util.function.Function;

public class OrderLineExpression<T, E, U extends OrderLine> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public OrderLineExpression(Expression<T, U> expression){
        super(expression);
    }

    public OrderLineExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public OrderLineExpression<T, U, U> updateId(Long id){
        return new OrderLineExpression(this, $it -> {((OrderLine)$it).__internalSet("id", id); return this;});
     }

     public OrderLineExpression<T, U, U> save(UserContext userContext){
        return new OrderLineExpression(this, $it -> ((OrderLine)$it).auditAs("Saved by Expression").save(userContext));
     }

     public OrderLineExpression<T, U, U> save(String intent, UserContext userContext){
        return new OrderLineExpression(this, $it -> ((OrderLine)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public CustomerOrderExpression<T, U, CustomerOrder> getCustomerOrder(){
       return new CustomerOrderExpression(this, $it ->  ((OrderLine)$it).getCustomerOrder());
    }

    public OrderLineExpression<T, U, U> updateCustomerOrder(CustomerOrder customerOrder){
       return new OrderLineExpression(this, $it ->  ((OrderLine)$it).updateCustomerOrder(customerOrder));
    }

    public ProductExpression<T, U, Product> getProduct(){
       return new ProductExpression(this, $it ->  ((OrderLine)$it).getProduct());
    }

    public OrderLineExpression<T, U, U> updateProduct(Product product){
       return new OrderLineExpression(this, $it ->  ((OrderLine)$it).updateProduct(product));
    }

    public Expression<T, String> getProductName(){
       return apply(OrderLine::getProductName);
    }
    public OrderLineExpression<T, U, U> updateProductName(String productName){
       return new OrderLineExpression(this, $it ->  ((OrderLine)$it).updateProductName(productName));
    }

    public Expression<T, String> getSku(){
       return apply(OrderLine::getSku);
    }
    public OrderLineExpression<T, U, U> updateSku(String sku){
       return new OrderLineExpression(this, $it ->  ((OrderLine)$it).updateSku(sku));
    }

    public Expression<T, Integer> getQuantity(){
       return apply(OrderLine::getQuantity);
    }
    public OrderLineExpression<T, U, U> updateQuantity(Integer quantity){
       return new OrderLineExpression(this, $it ->  ((OrderLine)$it).updateQuantity(quantity));
    }

    public CommercePlatformExpression<T, U, CommercePlatform> getCommercePlatform(){
       return new CommercePlatformExpression(this, $it ->  ((OrderLine)$it).getCommercePlatform());
    }

    public OrderLineExpression<T, U, U> updateCommercePlatform(CommercePlatform commercePlatform){
       return new OrderLineExpression(this, $it ->  ((OrderLine)$it).updateCommercePlatform(commercePlatform));
    }

    public Expression<T, LocalDateTime> getCreateTime(){
       return apply(OrderLine::getCreateTime);
    }
    public OrderLineExpression<T, U, U> updateCreateTime(LocalDateTime createTime){
       return new OrderLineExpression(this, $it ->  ((OrderLine)$it).updateCreateTime(createTime));
    }

}