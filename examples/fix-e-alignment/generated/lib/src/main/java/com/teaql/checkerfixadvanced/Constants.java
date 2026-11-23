
package com.teaql.checkerfixadvanced;

import com.teaql.checkerfixadvanced.tenant.Tenant;

public interface Constants  {
  public static final long TENANT_ID = 1l;
  public static final Tenant TENANT = Tenant.refer(TENANT_ID);
}