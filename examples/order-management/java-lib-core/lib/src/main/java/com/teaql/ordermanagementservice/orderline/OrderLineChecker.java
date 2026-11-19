
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

    public void checkAndFix(UserContext _context, OrderLine orderLine, ObjectLocation _parentLocation){
        if(needCheck(_context, orderLine)){
            markAsChecked(_context, orderLine);
            doCheck(_context, orderLine, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, OrderLine orderLine, ObjectLocation _parentLocation){
      if((orderLine == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, orderLine);
      if(orderLine.needPersist()){
      }
      if(orderLine.newItem()){
        if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "create_time"), () -> {
           orderLine.updateCreateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "create_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }
      }else if(orderLine.updateItem()){
        if(!orderLine.isPropertyLoaded("customer_order")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "customer_order"), "Mutation requires a fully loaded entity");
        }
        if(!orderLine.isPropertyLoaded("product")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "product"), "Mutation requires a fully loaded entity");
        }
        if(!orderLine.isPropertyLoaded("product_name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "product_name"), "Mutation requires a fully loaded entity");
        }
        if(!orderLine.isPropertyLoaded("sku")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "sku"), "Mutation requires a fully loaded entity");
        }
        if(!orderLine.isPropertyLoaded("quantity")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "quantity"), "Mutation requires a fully loaded entity");
        }
        if(!orderLine.isPropertyLoaded("commerce_platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "commerce_platform"), "Mutation requires a fully loaded entity");
        }
        if(!orderLine.isPropertyLoaded("create_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "create_time"), "Mutation requires a fully loaded entity");
        }

      }
      checkCustomerOrder(_context, orderLine.getProperty(OrderLine.CUSTOMER_ORDER_PROPERTY), newLocation(_parentLocation, "customer_order"));
      checkProduct(_context, orderLine.getProperty(OrderLine.PRODUCT_PROPERTY), newLocation(_parentLocation, "product"));
      checkProductName(_context, orderLine.getProperty(OrderLine.PRODUCT_NAME_PROPERTY), newLocation(_parentLocation, "product_name"));
      checkSku(_context, orderLine.getProperty(OrderLine.SKU_PROPERTY), newLocation(_parentLocation, "sku"));
      checkQuantity(_context, orderLine.getProperty(OrderLine.QUANTITY_PROPERTY), newLocation(_parentLocation, "quantity"));
      checkCommercePlatform(_context, orderLine.getProperty(OrderLine.COMMERCE_PLATFORM_PROPERTY), newLocation(_parentLocation, "commerce_platform"));
      checkCreateTime(_context, orderLine.getProperty(OrderLine.CREATE_TIME_PROPERTY), newLocation(_parentLocation, "create_time"));
    }

    public void checkCustomerOrder(UserContext _context, CustomerOrder customerOrder, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, customerOrder);
    if((customerOrder == null)){
        return;
    }
    new CustomerOrderChecker().checkAndFix(_context, customerOrder, _parentLocation);
    }
    public void checkProduct(UserContext _context, Product product, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, product);
    if((product == null)){
        return;
    }
    new ProductChecker().checkAndFix(_context, product, _parentLocation);
    }
    public void checkProductName(UserContext _context, String productName, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, productName);
    if((productName == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, productName);

    }
    public void checkSku(UserContext _context, String sku, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, sku);
    if((sku == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, sku);

    }
    public void checkQuantity(UserContext _context, Integer quantity, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, quantity);
    if((quantity == null)){
        return;
    }
    }
    public void checkCommercePlatform(UserContext _context, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, commercePlatform);
    if((commercePlatform == null)){
        return;
    }
    new CommercePlatformChecker().checkAndFix(_context, commercePlatform, _parentLocation);
    }
    public void checkCreateTime(UserContext _context, LocalDateTime createTime, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, createTime);
    if((createTime == null)){
        return;
    }
    }
}