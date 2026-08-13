
package com.teaql.ordermanagementservice;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.orderstatus.OrderStatus;

public interface Constants  {
  public static final long COMMERCE_PLATFORM_ID = 1l;
  public static final CommercePlatform COMMERCE_PLATFORM = CommercePlatform.refer(COMMERCE_PLATFORM_ID);
  public static final long ORDER_STATUS_PENDING_ID = 1001l ;
  public static final OrderStatus ORDER_STATUS_PENDING = OrderStatus.refer(ORDER_STATUS_PENDING_ID);public static final long ORDER_STATUS_PROCESSING_ID = 1002l ;
  public static final OrderStatus ORDER_STATUS_PROCESSING = OrderStatus.refer(ORDER_STATUS_PROCESSING_ID);public static final long ORDER_STATUS_SHIPPED_ID = 1003l ;
  public static final OrderStatus ORDER_STATUS_SHIPPED = OrderStatus.refer(ORDER_STATUS_SHIPPED_ID);public static final long ORDER_STATUS_COMPLETED_ID = 1004l ;
  public static final OrderStatus ORDER_STATUS_COMPLETED = OrderStatus.refer(ORDER_STATUS_COMPLETED_ID);
}