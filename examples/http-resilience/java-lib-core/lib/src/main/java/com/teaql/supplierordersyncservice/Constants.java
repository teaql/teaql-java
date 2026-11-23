
package com.teaql.supplierordersyncservice;

import com.teaql.supplierordersyncservice.supplier.Supplier;

public interface Constants  {
  public static final long SUPPLIER_ID = 1l;
  public static final Supplier SUPPLIER = Supplier.refer(SUPPLIER_ID);
}