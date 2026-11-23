
package com.teaql.purchaseordernumbering;

import com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.OrderNumber;
import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.teaql.purchaseordernumbering.platform.PlatformRequest<com.teaql.purchaseordernumbering.platform.Platform> platforms(){
      return new com.teaql.purchaseordernumbering.platform.PlatformRequest(com.teaql.purchaseordernumbering.platform.Platform.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.purchaseordernumbering.platform.PlatformRequest<com.teaql.purchaseordernumbering.platform.Platform> platformsWithMinimalFields(){
      return new com.teaql.purchaseordernumbering.platform.PlatformRequest(com.teaql.purchaseordernumbering.platform.Platform.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrderRequest<com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder> purchaseOrders(){
      return new com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrderRequest(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrderRequest<com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder> purchaseOrdersWithMinimalFields(){
      return new com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrderRequest(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.class).withVersion(Operator.GREATER_THAN, 0l);
  }

  public static com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrderRequest<com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder> purchaseOrderByOrderNumber(OrderNumber value){
      return purchaseOrders()
              .withOrderNumberIs(value)
              .limit(2);
  }


}