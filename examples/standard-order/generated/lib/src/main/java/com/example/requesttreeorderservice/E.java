
package com.example.requesttreeorderservice;

import com.example.requesttreeorderservice.customer.Customer;
import com.example.requesttreeorderservice.customer.CustomerExpression;
import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import com.example.requesttreeorderservice.customerorder.CustomerOrderExpression;
import com.example.requesttreeorderservice.orderitem.OrderItem;
import com.example.requesttreeorderservice.orderitem.OrderItemExpression;
import com.example.requesttreeorderservice.orderstatus.OrderStatus;
import com.example.requesttreeorderservice.orderstatus.OrderStatusExpression;
import com.example.requesttreeorderservice.platform.Platform;
import com.example.requesttreeorderservice.platform.PlatformExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static PlatformExpression<Platform, Platform, Platform> platform(Platform platform){
      return new PlatformExpression(new ValueExpression(platform, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.platform(<root>)"));
  }
  public static OrderStatusExpression<OrderStatus, OrderStatus, OrderStatus> orderStatus(OrderStatus orderStatus){
      return new OrderStatusExpression(new ValueExpression(orderStatus, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.orderStatus(<root>)"));
  }
  public static CustomerExpression<Customer, Customer, Customer> customer(Customer customer){
      return new CustomerExpression(new ValueExpression(customer, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.customer(<root>)"));
  }
  public static CustomerOrderExpression<CustomerOrder, CustomerOrder, CustomerOrder> customerOrder(CustomerOrder customerOrder){
      return new CustomerOrderExpression(new ValueExpression(customerOrder, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.customerOrder(<root>)"));
  }
  public static OrderItemExpression<OrderItem, OrderItem, OrderItem> orderItem(OrderItem orderItem){
      return new OrderItemExpression(new ValueExpression(orderItem, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.orderItem(<root>)"));
  }
}