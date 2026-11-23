
package com.teaql.supplieronboardingservice;

import com.teaql.supplieronboardingservice.warehouse.Warehouse;

public interface Constants  {
  public static final long WAREHOUSE_ID = 1l;
  public static final Warehouse WAREHOUSE = Warehouse.refer(WAREHOUSE_ID);
}