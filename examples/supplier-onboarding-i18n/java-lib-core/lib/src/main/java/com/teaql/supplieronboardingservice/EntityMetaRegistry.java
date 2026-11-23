
package com.teaql.supplieronboardingservice;

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
    registerWarehouse();
    registerSupplier();
  }
  private void registerWarehouse() {
      FieldLayout.installGenerated(com.teaql.supplieronboardingservice.warehouse.Warehouse.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.supplieronboardingservice.warehouse.Warehouse.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.supplieronboardingservice.warehouse.Warehouse.class);
      entityDescriptor.setEntitySupplier(com.teaql.supplieronboardingservice.warehouse.Warehouse::new);
      entityDescriptor.with("name", "Warehouse")

      .with("zh_CN", "仓库")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.supplieronboardingservice.warehouse.Warehouse.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.supplieronboardingservice.warehouse.Warehouse.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.supplieronboardingservice.warehouse.Warehouse.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.warehouse.Warehouse.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.warehouse.Warehouse.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.warehouse.Warehouse.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.warehouse.Warehouse.NAME_PROPERTY).with("isPassword", "false")

      .with("max", "100")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("candidates", "Central Warehouse")

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

      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.warehouse.Warehouse.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.warehouse.Warehouse.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerSupplier() {
      FieldLayout.installGenerated(com.teaql.supplieronboardingservice.supplier.Supplier.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.supplieronboardingservice.supplier.Supplier.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.supplieronboardingservice.supplier.Supplier.class);
      entityDescriptor.setEntitySupplier(com.teaql.supplieronboardingservice.supplier.Supplier::new);
      entityDescriptor.with("name", "Supplier")

      .with("zh_CN", "供应商")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.supplieronboardingservice.supplier.Supplier.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor warehouse = 
      entityDescriptor.addObjectProperty($factory, com.teaql.supplieronboardingservice.supplier.Supplier.WAREHOUSE_PROPERTY, com.teaql.supplieronboardingservice.warehouse.Warehouse.INTERNAL_TYPE, com.teaql.supplieronboardingservice.warehouse.Warehouse.SUPPLIER_LIST_PROPERTY, com.teaql.supplieronboardingservice.warehouse.Warehouse.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.supplieronboardingservice.supplier.Supplier.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor minimumOrderQuantity = 
      entityDescriptor.addSimpleProperty(com.teaql.supplieronboardingservice.supplier.Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY, Integer.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.supplieronboardingservice.supplier.Supplier.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.supplier.Supplier.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.supplier.Supplier.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.supplier.Supplier.WAREHOUSE_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.supplier.Supplier.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.supplier.Supplier.NAME_PROPERTY).with("isPassword", "false")

      .with("max", "80")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("sqlType", "VARCHAR(<max>)")

      .with("min", "1")

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

      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.supplier.Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.supplier.Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY).with("isPassword", "false")

      .with("max", "10000")

      .with("db2_sqlType", "decimal(19,7)")

      .with("isVersion", "false")

      .with("oracle_sqlType", "number(19,7)")

      .with("javaType", "java.lang.Integer")

      .with("sqlType", "INTEGER")

      .with("min", "10")

      .with("isId", "false")

      .with("isBool", "false")

      .with("isBaseEntityField", "false")

      .with("isNumber", "true")

      .with("isInt", "true")

      .with("isString", "false")

      .with("isDate", "false")

      .with("graphqlType", "Int")

      .with("isTime", "false")

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.supplier.Supplier.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.supplieronboardingservice.supplier.Supplier.VERSION_PROPERTY).with("isPassword", "false")

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