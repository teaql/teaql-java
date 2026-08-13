
package com.teaql.ordermanagementservice.orderline;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformChecker;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderChecker;
import com.teaql.ordermanagementservice.product.Product;
import com.teaql.ordermanagementservice.product.ProductChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.time.LocalDateTime;

public class OrderLineChecker implements Checker<OrderLine>{

    public String type(){
        return OrderLine.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _ctx, OrderLine orderLine, ObjectLocation _parentLocation){
        if(needCheck(_ctx, orderLine)){
            markAsChecked(_ctx, orderLine);
            doCheck(_ctx, orderLine, _parentLocation);
        }
    }

    public void doCheck(UserContext _ctx, OrderLine orderLine, ObjectLocation _parentLocation){
      if((orderLine == null)){
         return;
      }
      if(orderLine.newItem()){
        if(orderLine.getCreateTime() == null){
           orderLine.updateCreateTime(java.time.LocalDateTime.now());
        }
      }else if(orderLine.updateItem()){
      }
      checkCustomerOrder(_ctx, orderLine.getProperty(OrderLine.CUSTOMER_ORDER_PROPERTY), newLocation(_parentLocation, OrderLine.CUSTOMER_ORDER_PROPERTY));
      checkProduct(_ctx, orderLine.getProperty(OrderLine.PRODUCT_PROPERTY), newLocation(_parentLocation, OrderLine.PRODUCT_PROPERTY));
      checkProductName(_ctx, orderLine.getProperty(OrderLine.PRODUCT_NAME_PROPERTY), newLocation(_parentLocation, OrderLine.PRODUCT_NAME_PROPERTY));
      checkSku(_ctx, orderLine.getProperty(OrderLine.SKU_PROPERTY), newLocation(_parentLocation, OrderLine.SKU_PROPERTY));
      checkQuantity(_ctx, orderLine.getProperty(OrderLine.QUANTITY_PROPERTY), newLocation(_parentLocation, OrderLine.QUANTITY_PROPERTY));
      checkCommercePlatform(_ctx, orderLine.getProperty(OrderLine.COMMERCE_PLATFORM_PROPERTY), newLocation(_parentLocation, OrderLine.COMMERCE_PLATFORM_PROPERTY));
      checkCreateTime(_ctx, orderLine.getProperty(OrderLine.CREATE_TIME_PROPERTY), newLocation(_parentLocation, OrderLine.CREATE_TIME_PROPERTY));
    }

    public void checkCustomerOrder(UserContext _ctx, CustomerOrder customerOrder, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, customerOrder);
    if((customerOrder == null)){
        return;
    }
    new CustomerOrderChecker().checkAndFix(_ctx, customerOrder, _parentLocation);
    }
    public void checkProduct(UserContext _ctx, Product product, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, product);
    if((product == null)){
        return;
    }
    new ProductChecker().checkAndFix(_ctx, product, _parentLocation);
    }
    public void checkProductName(UserContext _ctx, String productName, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, productName);
    if((productName == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, productName);

    }
    public void checkSku(UserContext _ctx, String sku, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, sku);
    if((sku == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, sku);

    }
    public void checkQuantity(UserContext _ctx, Integer quantity, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, quantity);
    if((quantity == null)){
        return;
    }
    }
    public void checkCommercePlatform(UserContext _ctx, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, commercePlatform);
    if((commercePlatform == null)){
        return;
    }
    new CommercePlatformChecker().checkAndFix(_ctx, commercePlatform, _parentLocation);
    }
    public void checkCreateTime(UserContext _ctx, LocalDateTime createTime, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, createTime);
    if((createTime == null)){
        return;
    }
    }
}