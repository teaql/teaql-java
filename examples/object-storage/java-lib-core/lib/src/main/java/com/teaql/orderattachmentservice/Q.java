
package com.teaql.orderattachmentservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.teaql.orderattachmentservice.commerceplatform.CommercePlatformRequest<com.teaql.orderattachmentservice.commerceplatform.CommercePlatform> commercePlatforms(){
      return new com.teaql.orderattachmentservice.commerceplatform.CommercePlatformRequest(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.orderattachmentservice.commerceplatform.CommercePlatformRequest<com.teaql.orderattachmentservice.commerceplatform.CommercePlatform> commercePlatformsWithMinimalFields(){
      return new com.teaql.orderattachmentservice.commerceplatform.CommercePlatformRequest(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.orderattachmentservice.customerorder.CustomerOrderRequest<com.teaql.orderattachmentservice.customerorder.CustomerOrder> customerOrders(){
      return new com.teaql.orderattachmentservice.customerorder.CustomerOrderRequest(com.teaql.orderattachmentservice.customerorder.CustomerOrder.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.orderattachmentservice.customerorder.CustomerOrderRequest<com.teaql.orderattachmentservice.customerorder.CustomerOrder> customerOrdersWithMinimalFields(){
      return new com.teaql.orderattachmentservice.customerorder.CustomerOrderRequest(com.teaql.orderattachmentservice.customerorder.CustomerOrder.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.orderattachmentservice.orderattachment.OrderAttachmentRequest<com.teaql.orderattachmentservice.orderattachment.OrderAttachment> orderAttachments(){
      return new com.teaql.orderattachmentservice.orderattachment.OrderAttachmentRequest(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.orderattachmentservice.orderattachment.OrderAttachmentRequest<com.teaql.orderattachmentservice.orderattachment.OrderAttachment> orderAttachmentsWithMinimalFields(){
      return new com.teaql.orderattachmentservice.orderattachment.OrderAttachmentRequest(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}