
package com.teaql.ordereditreference;

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
    registerMerchant();
    registerSalesOrder();
    registerOrderItem();
  }
  private void registerMerchant() {
      FieldLayout.installGenerated(com.teaql.ordereditreference.merchant.Merchant.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.ordereditreference.merchant.Merchant.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.ordereditreference.merchant.Merchant.class);
      entityDescriptor.setEntitySupplier(com.teaql.ordereditreference.merchant.Merchant::new);
      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.ordereditreference.merchant.Merchant.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.ordereditreference.merchant.Merchant.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.ordereditreference.merchant.Merchant.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.ordereditreference.merchant.Merchant.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.ordereditreference.merchant.Merchant.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.ordereditreference.merchant.Merchant.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.ordereditreference.merchant.Merchant.NAME_PROPERTY).with("isPassword", "false")

      .with("max", "100")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("candidates", "Northwind Retail")

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

      entityDescriptor.findProperty(com.teaql.ordereditreference.merchant.Merchant.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.ordereditreference.merchant.Merchant.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerSalesOrder() {
      FieldLayout.installGenerated(com.teaql.ordereditreference.salesorder.SalesOrder.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.ordereditreference.salesorder.SalesOrder.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.ordereditreference.salesorder.SalesOrder.class);
      entityDescriptor.setEntitySupplier(com.teaql.ordereditreference.salesorder.SalesOrder::new);
      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.ordereditreference.salesorder.SalesOrder.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor merchant = 
      entityDescriptor.addObjectProperty($factory, com.teaql.ordereditreference.salesorder.SalesOrder.MERCHANT_PROPERTY, com.teaql.ordereditreference.merchant.Merchant.INTERNAL_TYPE, com.teaql.ordereditreference.merchant.Merchant.SALES_ORDER_LIST_PROPERTY, com.teaql.ordereditreference.merchant.Merchant.class)
      ;
      PropertyDescriptor orderNumber = 
      entityDescriptor.addSimpleProperty(com.teaql.ordereditreference.salesorder.SalesOrder.ORDER_NUMBER_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.ordereditreference.salesorder.SalesOrder.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.ordereditreference.salesorder.SalesOrder.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.ordereditreference.salesorder.SalesOrder.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.ordereditreference.salesorder.SalesOrder.MERCHANT_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.ordereditreference.salesorder.SalesOrder.ORDER_NUMBER_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.ordereditreference.salesorder.SalesOrder.ORDER_NUMBER_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.ordereditreference.salesorder.SalesOrder.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.ordereditreference.salesorder.SalesOrder.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerOrderItem() {
      FieldLayout.installGenerated(com.teaql.ordereditreference.orderitem.OrderItem.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.ordereditreference.orderitem.OrderItem.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.ordereditreference.orderitem.OrderItem.class);
      entityDescriptor.setEntitySupplier(com.teaql.ordereditreference.orderitem.OrderItem::new);
      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.ordereditreference.orderitem.OrderItem.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor salesOrder = 
      entityDescriptor.addObjectProperty($factory, com.teaql.ordereditreference.orderitem.OrderItem.SALES_ORDER_PROPERTY, com.teaql.ordereditreference.salesorder.SalesOrder.INTERNAL_TYPE, com.teaql.ordereditreference.salesorder.SalesOrder.ORDER_ITEM_LIST_PROPERTY, com.teaql.ordereditreference.salesorder.SalesOrder.class)
      ;
      PropertyDescriptor sku = 
      entityDescriptor.addSimpleProperty(com.teaql.ordereditreference.orderitem.OrderItem.SKU_PROPERTY, String.class)
      ;
      PropertyDescriptor quantity = 
      entityDescriptor.addSimpleProperty(com.teaql.ordereditreference.orderitem.OrderItem.QUANTITY_PROPERTY, Integer.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.ordereditreference.orderitem.OrderItem.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.ordereditreference.orderitem.OrderItem.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.ordereditreference.orderitem.OrderItem.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.ordereditreference.orderitem.OrderItem.SALES_ORDER_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.ordereditreference.orderitem.OrderItem.SKU_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.ordereditreference.orderitem.OrderItem.SKU_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.ordereditreference.orderitem.OrderItem.QUANTITY_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.ordereditreference.orderitem.OrderItem.QUANTITY_PROPERTY).with("isPassword", "false")

      .with("db2_sqlType", "decimal(19,7)")

      .with("isVersion", "false")

      .with("oracle_sqlType", "number(19,7)")

      .with("javaType", "java.lang.Integer")

      .with("sqlType", "INTEGER")

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

      entityDescriptor.findProperty(com.teaql.ordereditreference.orderitem.OrderItem.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.ordereditreference.orderitem.OrderItem.VERSION_PROPERTY).with("isPassword", "false")

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