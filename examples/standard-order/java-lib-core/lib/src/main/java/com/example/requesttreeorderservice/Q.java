
package com.example.requesttreeorderservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.example.requesttreeorderservice.platform.PlatformRequest<com.example.requesttreeorderservice.platform.Platform> platforms(){
      return new com.example.requesttreeorderservice.platform.PlatformRequest(com.example.requesttreeorderservice.platform.Platform.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.requesttreeorderservice.platform.PlatformRequest<com.example.requesttreeorderservice.platform.Platform> platformsWithMinimalFields(){
      return new com.example.requesttreeorderservice.platform.PlatformRequest(com.example.requesttreeorderservice.platform.Platform.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.example.requesttreeorderservice.orderstatus.OrderStatusRequest<com.example.requesttreeorderservice.orderstatus.OrderStatus> orderStatuses(){
      return new com.example.requesttreeorderservice.orderstatus.OrderStatusRequest(com.example.requesttreeorderservice.orderstatus.OrderStatus.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.requesttreeorderservice.orderstatus.OrderStatusRequest<com.example.requesttreeorderservice.orderstatus.OrderStatus> orderStatusesWithMinimalFields(){
      return new com.example.requesttreeorderservice.orderstatus.OrderStatusRequest(com.example.requesttreeorderservice.orderstatus.OrderStatus.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.example.requesttreeorderservice.customer.CustomerRequest<com.example.requesttreeorderservice.customer.Customer> customers(){
      return new com.example.requesttreeorderservice.customer.CustomerRequest(com.example.requesttreeorderservice.customer.Customer.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.requesttreeorderservice.customer.CustomerRequest<com.example.requesttreeorderservice.customer.Customer> customersWithMinimalFields(){
      return new com.example.requesttreeorderservice.customer.CustomerRequest(com.example.requesttreeorderservice.customer.Customer.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.example.requesttreeorderservice.customerorder.CustomerOrderRequest<com.example.requesttreeorderservice.customerorder.CustomerOrder> customerOrders(){
      return new com.example.requesttreeorderservice.customerorder.CustomerOrderRequest(com.example.requesttreeorderservice.customerorder.CustomerOrder.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.requesttreeorderservice.customerorder.CustomerOrderRequest<com.example.requesttreeorderservice.customerorder.CustomerOrder> customerOrdersWithMinimalFields(){
      return new com.example.requesttreeorderservice.customerorder.CustomerOrderRequest(com.example.requesttreeorderservice.customerorder.CustomerOrder.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.example.requesttreeorderservice.orderitem.OrderItemRequest<com.example.requesttreeorderservice.orderitem.OrderItem> orderItems(){
      return new com.example.requesttreeorderservice.orderitem.OrderItemRequest(com.example.requesttreeorderservice.orderitem.OrderItem.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.requesttreeorderservice.orderitem.OrderItemRequest<com.example.requesttreeorderservice.orderitem.OrderItem> orderItemsWithMinimalFields(){
      return new com.example.requesttreeorderservice.orderitem.OrderItemRequest(com.example.requesttreeorderservice.orderitem.OrderItem.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}