
package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.schooltype.SchoolType;

public interface Constants  {
  public static final long PLATFORM_ID = 1l;
  public static final Platform PLATFORM = Platform.refer(PLATFORM_ID);
  public static final long SCHOOL_TYPE_PRIMARY_ID = 1001l ;
  public static final SchoolType SCHOOL_TYPE_PRIMARY = SchoolType.refer(SCHOOL_TYPE_PRIMARY_ID);public static final long SCHOOL_TYPE_SECONDARY_ID = 1002l ;
  public static final SchoolType SCHOOL_TYPE_SECONDARY = SchoolType.refer(SCHOOL_TYPE_SECONDARY_ID);
}