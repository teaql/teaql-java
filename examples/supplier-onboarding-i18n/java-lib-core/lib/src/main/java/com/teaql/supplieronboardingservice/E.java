
package com.teaql.supplieronboardingservice;

import com.teaql.supplieronboardingservice.supplier.Supplier;
import com.teaql.supplieronboardingservice.supplier.SupplierExpression;
import com.teaql.supplieronboardingservice.warehouse.Warehouse;
import com.teaql.supplieronboardingservice.warehouse.WarehouseExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static WarehouseExpression<Warehouse, Warehouse, Warehouse> warehouse(Warehouse warehouse){
      return new WarehouseExpression(new ValueExpression(warehouse, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.warehouse(<root>)"));
  }
  public static SupplierExpression<Supplier, Supplier, Supplier> supplier(Supplier supplier){
      return new SupplierExpression(new ValueExpression(supplier, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.supplier(<root>)"));
  }
}