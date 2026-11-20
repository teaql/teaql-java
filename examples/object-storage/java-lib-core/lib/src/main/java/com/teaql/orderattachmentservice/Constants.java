
package com.teaql.orderattachmentservice;

import com.teaql.orderattachmentservice.commerceplatform.CommercePlatform;

public interface Constants  {
  public static final long COMMERCE_PLATFORM_ID = 1l;
  public static final CommercePlatform COMMERCE_PLATFORM = CommercePlatform.refer(COMMERCE_PLATFORM_ID);
}