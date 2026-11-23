
package com.teaql.supplieronboardingservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.teaql.supplieronboardingservice.warehouse.WarehouseRequest<com.teaql.supplieronboardingservice.warehouse.Warehouse> warehouses(){
      return new com.teaql.supplieronboardingservice.warehouse.WarehouseRequest(com.teaql.supplieronboardingservice.warehouse.Warehouse.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.supplieronboardingservice.warehouse.WarehouseRequest<com.teaql.supplieronboardingservice.warehouse.Warehouse> warehousesWithMinimalFields(){
      return new com.teaql.supplieronboardingservice.warehouse.WarehouseRequest(com.teaql.supplieronboardingservice.warehouse.Warehouse.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.supplieronboardingservice.supplier.SupplierRequest<com.teaql.supplieronboardingservice.supplier.Supplier> suppliers(){
      return new com.teaql.supplieronboardingservice.supplier.SupplierRequest(com.teaql.supplieronboardingservice.supplier.Supplier.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.supplieronboardingservice.supplier.SupplierRequest<com.teaql.supplieronboardingservice.supplier.Supplier> suppliersWithMinimalFields(){
      return new com.teaql.supplieronboardingservice.supplier.SupplierRequest(com.teaql.supplieronboardingservice.supplier.Supplier.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}