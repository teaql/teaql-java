
package com.teaql.ordermanagementservice.product;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformExpression;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderline.OrderLineListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.time.LocalDateTime;
import java.util.function.Function;

public class ProductExpression<T, E, U extends Product> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public ProductExpression(Expression<T, U> expression){
        super(expression);
    }

    public ProductExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public ProductExpression<T, U, U> updateId(Long id){
        return new ProductExpression(this, $it -> {((Product)$it).__internalSet("id", id); return this;});
     }

     public ProductExpression<T, U, U> save(UserContext userContext){
        return new ProductExpression(this, $it -> ((Product)$it).auditAs("Saved by Expression").save(userContext));
     }

     public ProductExpression<T, U, U> save(String intent, UserContext userContext){
        return new ProductExpression(this, $it -> ((Product)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", "getName", Product::getName);
    }
    public ProductExpression<T, U, U> updateName(String name){
       return new ProductExpression(this, $it ->  ((Product)$it).updateName(name));
    }

    public Expression<T, String> getSku(){
       return loaded("sku", "getSku", Product::getSku);
    }
    public ProductExpression<T, U, U> updateSku(String sku){
       return new ProductExpression(this, $it ->  ((Product)$it).updateSku(sku));
    }

    public Expression<T, String> getImageUrl(){
       return loaded("image_url", "getImageUrl", Product::getImageUrl);
    }
    public ProductExpression<T, U, U> updateImageUrl(String imageUrl){
       return new ProductExpression(this, $it ->  ((Product)$it).updateImageUrl(imageUrl));
    }

    public CommercePlatformExpression<T, U, CommercePlatform> getCommercePlatform(){
       return new CommercePlatformExpression(loaded("commerce_platform", "getCommercePlatform", Product::getCommercePlatform));
    }

    public ProductExpression<T, U, U> updateCommercePlatform(CommercePlatform commercePlatform){
       return new ProductExpression(this, $it ->  ((Product)$it).updateCommercePlatform(commercePlatform));
    }

    public Expression<T, LocalDateTime> getCreateTime(){
       return loaded("create_time", "getCreateTime", Product::getCreateTime);
    }
    public ProductExpression<T, U, U> updateCreateTime(LocalDateTime createTime){
       return new ProductExpression(this, $it ->  ((Product)$it).updateCreateTime(createTime));
    }

    public Expression<T, LocalDateTime> getUpdateTime(){
       return loaded("update_time", "getUpdateTime", Product::getUpdateTime);
    }
    public ProductExpression<T, U, U> updateUpdateTime(LocalDateTime updateTime){
       return new ProductExpression(this, $it ->  ((Product)$it).updateUpdateTime(updateTime));
    }

    public OrderLineListExpression<T, U, OrderLine> getOrderLineList(){
        return new OrderLineListExpression(loaded("order_line_list", "getOrderLineList", Product::getOrderLineList));
    }
    public ProductExpression<T, U, U> addOrderLine(OrderLine orderLine){
       return new ProductExpression(this, $it ->  ((Product)$it).addOrderLine(orderLine));
    }
}