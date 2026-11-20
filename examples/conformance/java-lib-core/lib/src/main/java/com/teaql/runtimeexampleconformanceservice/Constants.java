
package com.teaql.runtimeexampleconformanceservice;

import com.teaql.runtimeexampleconformanceservice.platform.Platform;

public interface Constants  {
  public static final long PLATFORM_ID = 1l;
  public static final Platform PLATFORM = Platform.refer(PLATFORM_ID);
}