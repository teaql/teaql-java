
package com.example.freightquotesettlement;

import com.example.freightquotesettlement.platform.Platform;

public interface Constants  {
  public static final long PLATFORM_ID = 1l;
  public static final Platform PLATFORM = Platform.refer(PLATFORM_ID);
}