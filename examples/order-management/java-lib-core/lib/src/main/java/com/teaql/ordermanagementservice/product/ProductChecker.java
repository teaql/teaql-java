
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

    public void checkAndFix(UserContext _ctx, Product product, ObjectLocation _parentLocation){
        if(needCheck(_ctx, product)){
            markAsChecked(_ctx, product);
            doCheck(_ctx, product, _parentLocation);
        }
    }

    public void doCheck(UserContext _ctx, Product product, ObjectLocation _parentLocation){
      if((product == null)){
         return;
      }
      if(product.newItem()){
        if(product.getCreateTime() == null){
           product.updateCreateTime(java.time.LocalDateTime.now());
        }if(product.getUpdateTime() == null){
           product.updateUpdateTime(java.time.LocalDateTime.now());
        }
      }else if(product.updateItem()){
        product.updateUpdateTime(java.time.LocalDateTime.now());
      }
      checkName(_ctx, product.getProperty(Product.NAME_PROPERTY), newLocation(_parentLocation, Product.NAME_PROPERTY));
      checkSku(_ctx, product.getProperty(Product.SKU_PROPERTY), newLocation(_parentLocation, Product.SKU_PROPERTY));
      checkImageUrl(_ctx, product.getProperty(Product.IMAGE_URL_PROPERTY), newLocation(_parentLocation, Product.IMAGE_URL_PROPERTY));
      checkCommercePlatform(_ctx, product.getProperty(Product.COMMERCE_PLATFORM_PROPERTY), newLocation(_parentLocation, Product.COMMERCE_PLATFORM_PROPERTY));
      checkCreateTime(_ctx, product.getProperty(Product.CREATE_TIME_PROPERTY), newLocation(_parentLocation, Product.CREATE_TIME_PROPERTY));
      checkUpdateTime(_ctx, product.getProperty(Product.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, Product.UPDATE_TIME_PROPERTY));
      for(int i = 0; product.getOrderLineList() != null && i < product.getOrderLineList().size(); i++){
         OrderLine orderLine = product.getOrderLineList().get(i);
         new OrderLineChecker().checkAndFix(_ctx, orderLine, newLocation(_parentLocation, Product.ORDER_LINE_LIST_PROPERTY, i));
      }
    }

    public void checkName(UserContext _ctx, String name, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, name);

    }
    public void checkSku(UserContext _ctx, String sku, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, sku);
    if((sku == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, sku);

    }
    public void checkImageUrl(UserContext _ctx, String imageUrl, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, imageUrl);
    if((imageUrl == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, imageUrl);

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
    public void checkUpdateTime(UserContext _ctx, LocalDateTime updateTime, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, updateTime);
    if((updateTime == null)){
        return;
    }
    }
}