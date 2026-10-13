
package com.example.requesttreeorderservice;

import com.example.requesttreeorderservice.orderstatus.OrderStatus;
import com.example.requesttreeorderservice.platform.Platform;

public interface Constants  {
  public static final long PLATFORM_ID = 1l;
  public static final Platform PLATFORM = Platform.refer(PLATFORM_ID);
  public static final long ORDER_STATUS_NEW_ID = 1001l ;
  public static final OrderStatus ORDER_STATUS_NEW = OrderStatus.refer(ORDER_STATUS_NEW_ID);public static final long ORDER_STATUS_PAID_ID = 1002l ;
  public static final OrderStatus ORDER_STATUS_PAID = OrderStatus.refer(ORDER_STATUS_PAID_ID);public static final long ORDER_STATUS_CANCELLED_ID = 1003l ;
  public static final OrderStatus ORDER_STATUS_CANCELLED = OrderStatus.refer(ORDER_STATUS_CANCELLED_ID);
}