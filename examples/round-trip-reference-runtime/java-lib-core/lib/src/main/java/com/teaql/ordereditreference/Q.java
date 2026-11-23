
package com.teaql.ordereditreference;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.teaql.ordereditreference.merchant.MerchantRequest<com.teaql.ordereditreference.merchant.Merchant> merchants(){
      return new com.teaql.ordereditreference.merchant.MerchantRequest(com.teaql.ordereditreference.merchant.Merchant.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.ordereditreference.merchant.MerchantRequest<com.teaql.ordereditreference.merchant.Merchant> merchantsWithMinimalFields(){
      return new com.teaql.ordereditreference.merchant.MerchantRequest(com.teaql.ordereditreference.merchant.Merchant.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.ordereditreference.salesorder.SalesOrderRequest<com.teaql.ordereditreference.salesorder.SalesOrder> salesOrders(){
      return new com.teaql.ordereditreference.salesorder.SalesOrderRequest(com.teaql.ordereditreference.salesorder.SalesOrder.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.ordereditreference.salesorder.SalesOrderRequest<com.teaql.ordereditreference.salesorder.SalesOrder> salesOrdersWithMinimalFields(){
      return new com.teaql.ordereditreference.salesorder.SalesOrderRequest(com.teaql.ordereditreference.salesorder.SalesOrder.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.ordereditreference.orderitem.OrderItemRequest<com.teaql.ordereditreference.orderitem.OrderItem> orderItems(){
      return new com.teaql.ordereditreference.orderitem.OrderItemRequest(com.teaql.ordereditreference.orderitem.OrderItem.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.ordereditreference.orderitem.OrderItemRequest<com.teaql.ordereditreference.orderitem.OrderItem> orderItemsWithMinimalFields(){
      return new com.teaql.ordereditreference.orderitem.OrderItemRequest(com.teaql.ordereditreference.orderitem.OrderItem.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}