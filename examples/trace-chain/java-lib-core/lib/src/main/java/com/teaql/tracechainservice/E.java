
package com.teaql.tracechainservice;

import com.teaql.tracechainservice.customerorder.CustomerOrder;
import com.teaql.tracechainservice.customerorder.CustomerOrderExpression;
import com.teaql.tracechainservice.orderitem.OrderItem;
import com.teaql.tracechainservice.orderitem.OrderItemExpression;
import com.teaql.tracechainservice.payment.Payment;
import com.teaql.tracechainservice.payment.PaymentExpression;
import com.teaql.tracechainservice.paymentattempt.PaymentAttempt;
import com.teaql.tracechainservice.paymentattempt.PaymentAttemptExpression;
import com.teaql.tracechainservice.platform.Platform;
import com.teaql.tracechainservice.platform.PlatformExpression;
import com.teaql.tracechainservice.shipment.Shipment;
import com.teaql.tracechainservice.shipment.ShipmentExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static PlatformExpression<Platform, Platform, Platform> platform(Platform platform){
      return new PlatformExpression(new ValueExpression(platform, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.platform(<root>)"));
  }
  public static CustomerOrderExpression<CustomerOrder, CustomerOrder, CustomerOrder> customerOrder(CustomerOrder customerOrder){
      return new CustomerOrderExpression(new ValueExpression(customerOrder, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.customerOrder(<root>)"));
  }
  public static OrderItemExpression<OrderItem, OrderItem, OrderItem> orderItem(OrderItem orderItem){
      return new OrderItemExpression(new ValueExpression(orderItem, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.orderItem(<root>)"));
  }
  public static PaymentExpression<Payment, Payment, Payment> payment(Payment payment){
      return new PaymentExpression(new ValueExpression(payment, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.payment(<root>)"));
  }
  public static PaymentAttemptExpression<PaymentAttempt, PaymentAttempt, PaymentAttempt> paymentAttempt(PaymentAttempt paymentAttempt){
      return new PaymentAttemptExpression(new ValueExpression(paymentAttempt, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.paymentAttempt(<root>)"));
  }
  public static ShipmentExpression<Shipment, Shipment, Shipment> shipment(Shipment shipment){
      return new ShipmentExpression(new ValueExpression(shipment, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.shipment(<root>)"));
  }
}