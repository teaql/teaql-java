
package com.teaql.supplierordersyncservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.teaql.supplierordersyncservice.supplier.SupplierRequest<com.teaql.supplierordersyncservice.supplier.Supplier> suppliers(){
      return new com.teaql.supplierordersyncservice.supplier.SupplierRequest(com.teaql.supplierordersyncservice.supplier.Supplier.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.supplierordersyncservice.supplier.SupplierRequest<com.teaql.supplierordersyncservice.supplier.Supplier> suppliersWithMinimalFields(){
      return new com.teaql.supplierordersyncservice.supplier.SupplierRequest(com.teaql.supplierordersyncservice.supplier.Supplier.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.supplierordersyncservice.supplierorder.SupplierOrderRequest<com.teaql.supplierordersyncservice.supplierorder.SupplierOrder> supplierOrders(){
      return new com.teaql.supplierordersyncservice.supplierorder.SupplierOrderRequest(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.supplierordersyncservice.supplierorder.SupplierOrderRequest<com.teaql.supplierordersyncservice.supplierorder.SupplierOrder> supplierOrdersWithMinimalFields(){
      return new com.teaql.supplierordersyncservice.supplierorder.SupplierOrderRequest(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}