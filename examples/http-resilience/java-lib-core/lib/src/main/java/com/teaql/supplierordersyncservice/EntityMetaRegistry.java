
package com.teaql.supplierordersyncservice;

import io.teaql.core.FieldLayout;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaAssembler;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;

public class EntityMetaRegistry implements EntityMetaAssembler {
  private EntityMetaFactory $factory;

  @Override
  public void assemble(EntityMetaFactory factory) {
    this.$factory = factory;
    registerSupplier();
    registerSupplierOrder();
  }
  private void registerSupplier() {
      FieldLayout.installGenerated(com.teaql.supplierordersyncservice.supplier.Supplier.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.supplierordersyncservice.supplier.Supplier.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.supplierordersyncservice.supplier.Supplier.class);
      entityDescriptor.setEntitySupplier(com.teaql.supplierordersyncservice.supplier.Supplier::new);
      entityDescriptor.with("name", "Supplier")

      .with("module", "Procurement")

      .with("module_key", "procurement")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.supplierordersyncservice.supplier.Supplier.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.supplierordersyncservice.supplier.Supplier.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.supplierordersyncservice.supplier.Supplier.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplier.Supplier.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplier.Supplier.ID_PROPERTY).with("isPassword", "false")

      .with("isVersion", "false")

      .with("oracle_sqlType", "number(11)")

      .with("javaType", "java.lang.Long")

      .with("sqlType", "BIGINT")

      .with("isId", "true")

      .with("isBaseEntityField", "true")

      .with("isBool", "false")

      .with("isNumber", "false")

      .with("isString", "false")

      .with("isDate", "false")

      .with("snowflake_sqlType", "number")

      .with("graphqlType", "Long")

      .with("isTime", "false")

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplier.Supplier.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplier.Supplier.NAME_PROPERTY).with("isPassword", "false")

      .with("max", "100")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("candidates", "Regional Supply Co")

      .with("sqlType", "VARCHAR(<max>)")

      .with("isId", "false")

      .with("isBool", "false")

      .with("isBaseEntityField", "false")

      .with("isNumber", "false")

      .with("isString", "true")

      .with("isDate", "false")

      .with("graphqlType", "String")

      .with("isTime", "false")

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplier.Supplier.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplier.Supplier.VERSION_PROPERTY).with("isPassword", "false")

      .with("isVersion", "true")

      .with("oracle_sqlType", "number(11)")

      .with("javaType", "java.lang.Long")

      .with("sqlType", "BIGINT")

      .with("isId", "false")

      .with("isBaseEntityField", "true")

      .with("isBool", "false")

      .with("isNumber", "false")

      .with("isString", "false")

      .with("isDate", "false")

      .with("snowflake_sqlType", "number")

      .with("graphqlType", "Long")

      .with("isTime", "false")

      .with("isText", "false")
      ;

      $factory.register(entityDescriptor);
  }
  private void registerSupplierOrder() {
      FieldLayout.installGenerated(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.class);
      entityDescriptor.setEntitySupplier(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder::new);
      entityDescriptor.with("name", "Supplier Order")

      .with("module", "Procurement")

      .with("module_key", "procurement")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor supplier = 
      entityDescriptor.addObjectProperty($factory, com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.SUPPLIER_PROPERTY, com.teaql.supplierordersyncservice.supplier.Supplier.INTERNAL_TYPE, com.teaql.supplierordersyncservice.supplier.Supplier.SUPPLIER_ORDER_LIST_PROPERTY, com.teaql.supplierordersyncservice.supplier.Supplier.class)
      ;
      PropertyDescriptor externalReference = 
      entityDescriptor.addSimpleProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.EXTERNAL_REFERENCE_PROPERTY, String.class)
      ;
      PropertyDescriptor syncStatus = 
      entityDescriptor.addSimpleProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.SYNC_STATUS_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.ID_PROPERTY).with("isPassword", "false")

      .with("isVersion", "false")

      .with("oracle_sqlType", "number(11)")

      .with("javaType", "java.lang.Long")

      .with("sqlType", "BIGINT")

      .with("isId", "true")

      .with("isBaseEntityField", "true")

      .with("isBool", "false")

      .with("isNumber", "false")

      .with("isString", "false")

      .with("isDate", "false")

      .with("snowflake_sqlType", "number")

      .with("graphqlType", "Long")

      .with("isTime", "false")

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.SUPPLIER_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.EXTERNAL_REFERENCE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.EXTERNAL_REFERENCE_PROPERTY).with("isPassword", "false")

      .with("max", "100")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("sqlType", "VARCHAR(<max>)")

      .with("isId", "false")

      .with("isBool", "false")

      .with("isBaseEntityField", "false")

      .with("isNumber", "false")

      .with("isString", "true")

      .with("isDate", "false")

      .with("graphqlType", "String")

      .with("isTime", "false")

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.SYNC_STATUS_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.SYNC_STATUS_PROPERTY).with("isPassword", "false")

      .with("max", "100")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("sqlType", "VARCHAR(<max>)")

      .with("isId", "false")

      .with("isBool", "false")

      .with("isBaseEntityField", "false")

      .with("isNumber", "false")

      .with("isString", "true")

      .with("isDate", "false")

      .with("graphqlType", "String")

      .with("isTime", "false")

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.VERSION_PROPERTY).with("isPassword", "false")

      .with("isVersion", "true")

      .with("oracle_sqlType", "number(11)")

      .with("javaType", "java.lang.Long")

      .with("sqlType", "BIGINT")

      .with("isId", "false")

      .with("isBaseEntityField", "true")

      .with("isBool", "false")

      .with("isNumber", "false")

      .with("isString", "false")

      .with("isDate", "false")

      .with("snowflake_sqlType", "number")

      .with("graphqlType", "Long")

      .with("isTime", "false")

      .with("isText", "false")
      ;

      $factory.register(entityDescriptor);
  }
}