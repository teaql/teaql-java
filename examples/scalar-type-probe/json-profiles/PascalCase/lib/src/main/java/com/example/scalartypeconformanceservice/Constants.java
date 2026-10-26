
package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.typeplatform.TypePlatform;

public interface Constants  {
  public static final long TYPE_PLATFORM_ID = 1l;
  public static final TypePlatform TYPE_PLATFORM = TypePlatform.refer(TYPE_PLATFORM_ID);
}