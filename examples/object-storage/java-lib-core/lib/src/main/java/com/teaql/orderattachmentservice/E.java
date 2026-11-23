
package com.teaql.orderattachmentservice;

import com.teaql.orderattachmentservice.commerceplatform.CommercePlatform;
import com.teaql.orderattachmentservice.commerceplatform.CommercePlatformExpression;
import com.teaql.orderattachmentservice.customerorder.CustomerOrder;
import com.teaql.orderattachmentservice.customerorder.CustomerOrderExpression;
import com.teaql.orderattachmentservice.orderattachment.OrderAttachment;
import com.teaql.orderattachmentservice.orderattachment.OrderAttachmentExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static CommercePlatformExpression<CommercePlatform, CommercePlatform, CommercePlatform> commercePlatform(CommercePlatform commercePlatform){
      return new CommercePlatformExpression(new ValueExpression(commercePlatform, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.commercePlatform(<root>)"));
  }
  public static CustomerOrderExpression<CustomerOrder, CustomerOrder, CustomerOrder> customerOrder(CustomerOrder customerOrder){
      return new CustomerOrderExpression(new ValueExpression(customerOrder, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.customerOrder(<root>)"));
  }
  public static OrderAttachmentExpression<OrderAttachment, OrderAttachment, OrderAttachment> orderAttachment(OrderAttachment orderAttachment){
      return new OrderAttachmentExpression(new ValueExpression(orderAttachment, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.orderAttachment(<root>)"));
  }
}