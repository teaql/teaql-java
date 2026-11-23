
package com.teaql.supplierordersyncservice;

import com.teaql.supplierordersyncservice.supplier.Supplier;
import com.teaql.supplierordersyncservice.supplier.SupplierExpression;
import com.teaql.supplierordersyncservice.supplierorder.SupplierOrder;
import com.teaql.supplierordersyncservice.supplierorder.SupplierOrderExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static SupplierExpression<Supplier, Supplier, Supplier> supplier(Supplier supplier){
      return new SupplierExpression(new ValueExpression(supplier, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.supplier(<root>)"));
  }
  public static SupplierOrderExpression<SupplierOrder, SupplierOrder, SupplierOrder> supplierOrder(SupplierOrder supplierOrder){
      return new SupplierOrderExpression(new ValueExpression(supplierOrder, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.supplierOrder(<root>)"));
  }
}