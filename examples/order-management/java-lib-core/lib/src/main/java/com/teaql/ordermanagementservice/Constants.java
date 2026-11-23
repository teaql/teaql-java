
package com.teaql.ordermanagementservice;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.orderstatus.OrderStatus;

public interface Constants  {
  public static final long COMMERCE_PLATFORM_ID = 1l;
  public static final CommercePlatform COMMERCE_PLATFORM = CommercePlatform.refer(COMMERCE_PLATFORM_ID);
  public static final long ORDER_STATUS_PENDING_ID = 1001l ;
  public static final OrderStatus ORDER_STATUS_PENDING = OrderStatus.refer(ORDER_STATUS_PENDING_ID);public static final long ORDER_STATUS_CONFIRMED_ID = 1002l ;
  public static final OrderStatus ORDER_STATUS_CONFIRMED = OrderStatus.refer(ORDER_STATUS_CONFIRMED_ID);
}