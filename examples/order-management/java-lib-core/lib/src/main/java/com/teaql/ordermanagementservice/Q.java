
package com.teaql.ordermanagementservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.teaql.ordermanagementservice.commerceplatform.CommercePlatformRequest<com.teaql.ordermanagementservice.commerceplatform.CommercePlatform> commercePlatforms(){
      return new com.teaql.ordermanagementservice.commerceplatform.CommercePlatformRequest(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.ordermanagementservice.commerceplatform.CommercePlatformRequest<com.teaql.ordermanagementservice.commerceplatform.CommercePlatform> commercePlatformsWithMinimalFields(){
      return new com.teaql.ordermanagementservice.commerceplatform.CommercePlatformRequest(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.class).withVersion(Operator.GREATER_THAN, 0l);
  }


  public static com.teaql.ordermanagementservice.customer.CustomerRequest<com.teaql.ordermanagementservice.customer.Customer> customers(){
      return new com.teaql.ordermanagementservice.customer.CustomerRequest(com.teaql.ordermanagementservice.customer.Customer.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.ordermanagementservice.customer.CustomerRequest<com.teaql.ordermanagementservice.customer.Customer> customersWithMinimalFields(){
      return new com.teaql.ordermanagementservice.customer.CustomerRequest(com.teaql.ordermanagementservice.customer.Customer.class).withVersion(Operator.GREATER_THAN, 0l);
  }


  public static com.teaql.ordermanagementservice.orderstatus.OrderStatusRequest<com.teaql.ordermanagementservice.orderstatus.OrderStatus> orderStatuses(){
      return new com.teaql.ordermanagementservice.orderstatus.OrderStatusRequest(com.teaql.ordermanagementservice.orderstatus.OrderStatus.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.ordermanagementservice.orderstatus.OrderStatusRequest<com.teaql.ordermanagementservice.orderstatus.OrderStatus> orderStatusesWithMinimalFields(){
      return new com.teaql.ordermanagementservice.orderstatus.OrderStatusRequest(com.teaql.ordermanagementservice.orderstatus.OrderStatus.class).withVersion(Operator.GREATER_THAN, 0l);
  }


  public static com.teaql.ordermanagementservice.customerorder.CustomerOrderRequest<com.teaql.ordermanagementservice.customerorder.CustomerOrder> customerOrders(){
      return new com.teaql.ordermanagementservice.customerorder.CustomerOrderRequest(com.teaql.ordermanagementservice.customerorder.CustomerOrder.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.ordermanagementservice.customerorder.CustomerOrderRequest<com.teaql.ordermanagementservice.customerorder.CustomerOrder> customerOrdersWithMinimalFields(){
      return new com.teaql.ordermanagementservice.customerorder.CustomerOrderRequest(com.teaql.ordermanagementservice.customerorder.CustomerOrder.class).withVersion(Operator.GREATER_THAN, 0l);
  }


  public static com.teaql.ordermanagementservice.product.ProductRequest<com.teaql.ordermanagementservice.product.Product> products(){
      return new com.teaql.ordermanagementservice.product.ProductRequest(com.teaql.ordermanagementservice.product.Product.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.ordermanagementservice.product.ProductRequest<com.teaql.ordermanagementservice.product.Product> productsWithMinimalFields(){
      return new com.teaql.ordermanagementservice.product.ProductRequest(com.teaql.ordermanagementservice.product.Product.class).withVersion(Operator.GREATER_THAN, 0l);
  }


  public static com.teaql.ordermanagementservice.orderline.OrderLineRequest<com.teaql.ordermanagementservice.orderline.OrderLine> orderLines(){
      return new com.teaql.ordermanagementservice.orderline.OrderLineRequest(com.teaql.ordermanagementservice.orderline.OrderLine.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.ordermanagementservice.orderline.OrderLineRequest<com.teaql.ordermanagementservice.orderline.OrderLine> orderLinesWithMinimalFields(){
      return new com.teaql.ordermanagementservice.orderline.OrderLineRequest(com.teaql.ordermanagementservice.orderline.OrderLine.class).withVersion(Operator.GREATER_THAN, 0l);
  }


  public static com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPresetRequest<com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset> orderSearchPresets(){
      return new com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPresetRequest(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPresetRequest<com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset> orderSearchPresetsWithMinimalFields(){
      return new com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPresetRequest(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.class).withVersion(Operator.GREATER_THAN, 0l);
  }


}