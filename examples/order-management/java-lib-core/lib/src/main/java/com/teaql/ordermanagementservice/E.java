
package com.teaql.ordermanagementservice;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformExpression;
import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.customer.CustomerExpression;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderExpression;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderline.OrderLineExpression;
import com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset;
import com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPresetExpression;
import com.teaql.ordermanagementservice.orderstatus.OrderStatus;
import com.teaql.ordermanagementservice.orderstatus.OrderStatusExpression;
import com.teaql.ordermanagementservice.product.Product;
import com.teaql.ordermanagementservice.product.ProductExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static CommercePlatformExpression<CommercePlatform, CommercePlatform, CommercePlatform> commercePlatform(CommercePlatform commercePlatform){
      return new CommercePlatformExpression(new ValueExpression(commercePlatform, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.commercePlatform(<root>)"));
  }
  public static CustomerExpression<Customer, Customer, Customer> customer(Customer customer){
      return new CustomerExpression(new ValueExpression(customer, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.customer(<root>)"));
  }
  public static OrderStatusExpression<OrderStatus, OrderStatus, OrderStatus> orderStatus(OrderStatus orderStatus){
      return new OrderStatusExpression(new ValueExpression(orderStatus, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.orderStatus(<root>)"));
  }
  public static CustomerOrderExpression<CustomerOrder, CustomerOrder, CustomerOrder> customerOrder(CustomerOrder customerOrder){
      return new CustomerOrderExpression(new ValueExpression(customerOrder, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.customerOrder(<root>)"));
  }
  public static ProductExpression<Product, Product, Product> product(Product product){
      return new ProductExpression(new ValueExpression(product, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.product(<root>)"));
  }
  public static OrderLineExpression<OrderLine, OrderLine, OrderLine> orderLine(OrderLine orderLine){
      return new OrderLineExpression(new ValueExpression(orderLine, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.orderLine(<root>)"));
  }
  public static OrderSearchPresetExpression<OrderSearchPreset, OrderSearchPreset, OrderSearchPreset> orderSearchPreset(OrderSearchPreset orderSearchPreset){
      return new OrderSearchPresetExpression(new ValueExpression(orderSearchPreset, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.orderSearchPreset(<root>)"));
  }
}