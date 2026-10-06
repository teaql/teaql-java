
package com.teaql.tracechainservice;

import com.teaql.tracechainservice.platform.Platform;

public interface Constants  {
  public static final long PLATFORM_ID = 1l;
  public static final Platform PLATFORM = Platform.refer(PLATFORM_ID);
}