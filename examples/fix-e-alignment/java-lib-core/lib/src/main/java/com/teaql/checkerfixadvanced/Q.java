
package com.teaql.checkerfixadvanced;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.teaql.checkerfixadvanced.tenant.TenantRequest<com.teaql.checkerfixadvanced.tenant.Tenant> tenants(){
      return new com.teaql.checkerfixadvanced.tenant.TenantRequest(com.teaql.checkerfixadvanced.tenant.Tenant.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.checkerfixadvanced.tenant.TenantRequest<com.teaql.checkerfixadvanced.tenant.Tenant> tenantsWithMinimalFields(){
      return new com.teaql.checkerfixadvanced.tenant.TenantRequest(com.teaql.checkerfixadvanced.tenant.Tenant.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrderRequest<com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder> purchaseOrders(){
      return new com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrderRequest(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrderRequest<com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder> purchaseOrdersWithMinimalFields(){
      return new com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrderRequest(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.checkerfixadvanced.orderitem.OrderItemRequest<com.teaql.checkerfixadvanced.orderitem.OrderItem> orderItems(){
      return new com.teaql.checkerfixadvanced.orderitem.OrderItemRequest(com.teaql.checkerfixadvanced.orderitem.OrderItem.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.checkerfixadvanced.orderitem.OrderItemRequest<com.teaql.checkerfixadvanced.orderitem.OrderItem> orderItemsWithMinimalFields(){
      return new com.teaql.checkerfixadvanced.orderitem.OrderItemRequest(com.teaql.checkerfixadvanced.orderitem.OrderItem.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}