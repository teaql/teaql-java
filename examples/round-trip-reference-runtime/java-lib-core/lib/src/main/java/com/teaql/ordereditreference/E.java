
package com.teaql.ordereditreference;

import com.teaql.ordereditreference.merchant.Merchant;
import com.teaql.ordereditreference.merchant.MerchantExpression;
import com.teaql.ordereditreference.orderitem.OrderItem;
import com.teaql.ordereditreference.orderitem.OrderItemExpression;
import com.teaql.ordereditreference.salesorder.SalesOrder;
import com.teaql.ordereditreference.salesorder.SalesOrderExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static MerchantExpression<Merchant, Merchant, Merchant> merchant(Merchant merchant){
      return new MerchantExpression(new ValueExpression(merchant, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.merchant(<root>)"));
  }
  public static SalesOrderExpression<SalesOrder, SalesOrder, SalesOrder> salesOrder(SalesOrder salesOrder){
      return new SalesOrderExpression(new ValueExpression(salesOrder, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.salesOrder(<root>)"));
  }
  public static OrderItemExpression<OrderItem, OrderItem, OrderItem> orderItem(OrderItem orderItem){
      return new OrderItemExpression(new ValueExpression(orderItem, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.orderItem(<root>)"));
  }
}