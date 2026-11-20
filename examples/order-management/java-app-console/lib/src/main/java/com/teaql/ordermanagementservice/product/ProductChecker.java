
package com.teaql.ordermanagementservice.product;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformChecker;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderline.OrderLineChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.time.LocalDateTime;

public class ProductChecker implements Checker<Product>{

    public String type(){
        return Product.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Product product, ObjectLocation _parentLocation){
        if(needCheck(_context, product)){
            markAsChecked(_context, product);
            doCheck(_context, product, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Product product, ObjectLocation _parentLocation){
      if((product == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, product);
      if(product.needPersist()){
      }
      if(product.newItem()){
        if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "create_time"), () -> {
           product.updateCreateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "create_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "update_time"), () -> {
           product.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }
      }else if(product.updateItem()){
        if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "update_time"), () -> {
           product.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }
        if(!product.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }
        if(!product.isPropertyLoaded("sku")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "sku"), "Mutation requires a fully loaded entity");
        }
        if(!product.isPropertyLoaded("image_url")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "image_url"), "Mutation requires a fully loaded entity");
        }
        if(!product.isPropertyLoaded("commerce_platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "commerce_platform"), "Mutation requires a fully loaded entity");
        }
        if(!product.isPropertyLoaded("create_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "create_time"), "Mutation requires a fully loaded entity");
        }
        if(!product.isPropertyLoaded("update_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "update_time"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, product.getProperty(Product.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      checkSku(_context, product.getProperty(Product.SKU_PROPERTY), newLocation(_parentLocation, "sku"));
      checkImageUrl(_context, product.getProperty(Product.IMAGE_URL_PROPERTY), newLocation(_parentLocation, "image_url"));
      checkCommercePlatform(_context, product.getProperty(Product.COMMERCE_PLATFORM_PROPERTY), newLocation(_parentLocation, "commerce_platform"));
      checkCreateTime(_context, product.getProperty(Product.CREATE_TIME_PROPERTY), newLocation(_parentLocation, "create_time"));
      checkUpdateTime(_context, product.getProperty(Product.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, "update_time"));
      for(int i = 0; product.getOrderLineList() != null && i < product.getOrderLineList().size(); i++){
         OrderLine orderLine = product.getOrderLineList().get(i);
         new OrderLineChecker().checkAndFix(_context, orderLine, newLocation(_parentLocation, "order_line_list", i));
      }
    }

    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

    }
    public void checkSku(UserContext _context, String sku, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, sku);
    if((sku == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, sku);

    }
    public void checkImageUrl(UserContext _context, String imageUrl, ObjectLocation _parentLocation){
    if((imageUrl == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, imageUrl);

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
    public void checkUpdateTime(UserContext _context, LocalDateTime updateTime, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, updateTime);
    if((updateTime == null)){
        return;
    }
    }
}