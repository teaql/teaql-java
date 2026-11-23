
package com.teaql.tracechainservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.teaql.tracechainservice.platform.PlatformRequest<com.teaql.tracechainservice.platform.Platform> platforms(){
      return new com.teaql.tracechainservice.platform.PlatformRequest(com.teaql.tracechainservice.platform.Platform.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.tracechainservice.platform.PlatformRequest<com.teaql.tracechainservice.platform.Platform> platformsWithMinimalFields(){
      return new com.teaql.tracechainservice.platform.PlatformRequest(com.teaql.tracechainservice.platform.Platform.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.tracechainservice.customerorder.CustomerOrderRequest<com.teaql.tracechainservice.customerorder.CustomerOrder> customerOrders(){
      return new com.teaql.tracechainservice.customerorder.CustomerOrderRequest(com.teaql.tracechainservice.customerorder.CustomerOrder.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.tracechainservice.customerorder.CustomerOrderRequest<com.teaql.tracechainservice.customerorder.CustomerOrder> customerOrdersWithMinimalFields(){
      return new com.teaql.tracechainservice.customerorder.CustomerOrderRequest(com.teaql.tracechainservice.customerorder.CustomerOrder.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.tracechainservice.orderitem.OrderItemRequest<com.teaql.tracechainservice.orderitem.OrderItem> orderItems(){
      return new com.teaql.tracechainservice.orderitem.OrderItemRequest(com.teaql.tracechainservice.orderitem.OrderItem.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.tracechainservice.orderitem.OrderItemRequest<com.teaql.tracechainservice.orderitem.OrderItem> orderItemsWithMinimalFields(){
      return new com.teaql.tracechainservice.orderitem.OrderItemRequest(com.teaql.tracechainservice.orderitem.OrderItem.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.tracechainservice.payment.PaymentRequest<com.teaql.tracechainservice.payment.Payment> payments(){
      return new com.teaql.tracechainservice.payment.PaymentRequest(com.teaql.tracechainservice.payment.Payment.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.tracechainservice.payment.PaymentRequest<com.teaql.tracechainservice.payment.Payment> paymentsWithMinimalFields(){
      return new com.teaql.tracechainservice.payment.PaymentRequest(com.teaql.tracechainservice.payment.Payment.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.tracechainservice.paymentattempt.PaymentAttemptRequest<com.teaql.tracechainservice.paymentattempt.PaymentAttempt> paymentAttempts(){
      return new com.teaql.tracechainservice.paymentattempt.PaymentAttemptRequest(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.tracechainservice.paymentattempt.PaymentAttemptRequest<com.teaql.tracechainservice.paymentattempt.PaymentAttempt> paymentAttemptsWithMinimalFields(){
      return new com.teaql.tracechainservice.paymentattempt.PaymentAttemptRequest(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.tracechainservice.shipment.ShipmentRequest<com.teaql.tracechainservice.shipment.Shipment> shipments(){
      return new com.teaql.tracechainservice.shipment.ShipmentRequest(com.teaql.tracechainservice.shipment.Shipment.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.tracechainservice.shipment.ShipmentRequest<com.teaql.tracechainservice.shipment.Shipment> shipmentsWithMinimalFields(){
      return new com.teaql.tracechainservice.shipment.ShipmentRequest(com.teaql.tracechainservice.shipment.Shipment.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}