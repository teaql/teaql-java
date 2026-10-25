
package com.teaql.checkerfixadvanced;

import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.orderitem.OrderItemExpression;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrderExpression;
import com.teaql.checkerfixadvanced.tenant.Tenant;
import com.teaql.checkerfixadvanced.tenant.TenantExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static TenantExpression<Tenant, Tenant, Tenant> tenant(Tenant tenant){
      return new TenantExpression(new ValueExpression(tenant, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.tenant(<root>)"));
  }
  public static PurchaseOrderExpression<PurchaseOrder, PurchaseOrder, PurchaseOrder> purchaseOrder(PurchaseOrder purchaseOrder){
      return new PurchaseOrderExpression(new ValueExpression(purchaseOrder, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.purchaseOrder(<root>)"));
  }
  public static OrderItemExpression<OrderItem, OrderItem, OrderItem> orderItem(OrderItem orderItem){
      return new OrderItemExpression(new ValueExpression(orderItem, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.orderItem(<root>)"));
  }
}