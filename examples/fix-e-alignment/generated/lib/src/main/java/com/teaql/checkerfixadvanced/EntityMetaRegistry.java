
package com.teaql.checkerfixadvanced;

import io.teaql.core.FieldLayout;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaAssembler;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EntityMetaRegistry implements EntityMetaAssembler {
  private EntityMetaFactory $factory;

  @Override
  public void assemble(EntityMetaFactory factory) {
    this.$factory = factory;
    registerTenant();
    registerPurchaseOrder();
    registerOrderItem();
  }
  private void registerTenant() {
      FieldLayout.installGenerated(com.teaql.checkerfixadvanced.tenant.Tenant.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.checkerfixadvanced.tenant.Tenant.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.checkerfixadvanced.tenant.Tenant.class);
      entityDescriptor.setEntitySupplier(com.teaql.checkerfixadvanced.tenant.Tenant::new);
      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.tenant.Tenant.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.tenant.Tenant.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.tenant.Tenant.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.tenant.Tenant.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.tenant.Tenant.ID_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.tenant.Tenant.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.tenant.Tenant.NAME_PROPERTY).with("isPassword", "false")
      .with("max", "80")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "Tenant Name")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.tenant.Tenant.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.tenant.Tenant.VERSION_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      $factory.register(entityDescriptor);
  }
  private void registerPurchaseOrder() {
      FieldLayout.installGenerated(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.class);
      entityDescriptor.setEntitySupplier(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder::new);
      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor tenant = 
      entityDescriptor.addObjectProperty($factory, com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.TENANT_PROPERTY, com.teaql.checkerfixadvanced.tenant.Tenant.INTERNAL_TYPE, com.teaql.checkerfixadvanced.tenant.Tenant.PURCHASE_ORDER_LIST_PROPERTY, com.teaql.checkerfixadvanced.tenant.Tenant.class)
      ;
      PropertyDescriptor contactPhone = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.CONTACT_PHONE_PROPERTY, String.class)
      ;
      PropertyDescriptor internalNote = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.INTERNAL_NOTE_PROPERTY, String.class)
      ;
      PropertyDescriptor createTime = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.CREATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor updateTime = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.UPDATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.ID_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.TENANT_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.TENANT_PROPERTY).with("contextRoot", "true");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.CONTACT_PHONE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.CONTACT_PHONE_PROPERTY).with("isPassword", "false")
      .with("max", "32")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.INTERNAL_NOTE_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("optional", "true")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.CREATE_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.CREATE_TIME_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("javaType", "java.time.LocalDateTime")
      .with("sqlType", "TIMESTAMP")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "false")
      .with("mssql_sqlType", "dateTime")
      .with("isDateTime", "true")
      .with("createFunction", "now")
      .with("isDate", "true")
      .with("isString", "false")
      .with("graphqlType", "LocalTime")
      .with("isTime", "true")
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.UPDATE_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.UPDATE_TIME_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("updateFunction", "now")
      .with("javaType", "java.time.LocalDateTime")
      .with("sqlType", "TIMESTAMP")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "false")
      .with("mssql_sqlType", "dateTime")
      .with("isDateTime", "true")
      .with("createFunction", "now")
      .with("isDate", "true")
      .with("isString", "false")
      .with("graphqlType", "LocalTime")
      .with("isTime", "true")
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.VERSION_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      $factory.register(entityDescriptor);
  }
  private void registerOrderItem() {
      FieldLayout.installGenerated(com.teaql.checkerfixadvanced.orderitem.OrderItem.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.checkerfixadvanced.orderitem.OrderItem.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.checkerfixadvanced.orderitem.OrderItem.class);
      entityDescriptor.setEntitySupplier(com.teaql.checkerfixadvanced.orderitem.OrderItem::new);
      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor purchaseOrder = 
      entityDescriptor.addObjectProperty($factory, com.teaql.checkerfixadvanced.orderitem.OrderItem.PURCHASE_ORDER_PROPERTY, com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.INTERNAL_TYPE, com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.ORDER_ITEM_LIST_PROPERTY, com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder.class)
      ;
      PropertyDescriptor tenant = 
      entityDescriptor.addObjectProperty($factory, com.teaql.checkerfixadvanced.orderitem.OrderItem.TENANT_PROPERTY, com.teaql.checkerfixadvanced.tenant.Tenant.INTERNAL_TYPE, com.teaql.checkerfixadvanced.tenant.Tenant.ORDER_ITEM_LIST_PROPERTY, com.teaql.checkerfixadvanced.tenant.Tenant.class)
      ;
      PropertyDescriptor productName = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.PRODUCT_NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor unitPrice = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.UNIT_PRICE_PROPERTY, BigDecimal.class)
      ;
      PropertyDescriptor createTime = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.CREATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor updateTime = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.UPDATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.ID_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.PURCHASE_ORDER_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.TENANT_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.TENANT_PROPERTY).with("contextRoot", "true");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.PRODUCT_NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.PRODUCT_NAME_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.UNIT_PRICE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.UNIT_PRICE_PROPERTY).with("isPassword", "false")
      .with("max", "1000000")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("oracle_sqlType", "number(19,7)")
      .with("javaType", "java.math.BigDecimal")
      .with("sqlType", "NUMERIC(19,7)")
      .with("min", "0.01")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "true")
      .with("isString", "false")
      .with("isDate", "false")
      .with("graphqlType", "BigDecimal")
      .with("isTime", "false")
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.CREATE_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.CREATE_TIME_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("javaType", "java.time.LocalDateTime")
      .with("sqlType", "TIMESTAMP")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "false")
      .with("mssql_sqlType", "dateTime")
      .with("isDateTime", "true")
      .with("createFunction", "now")
      .with("isDate", "true")
      .with("isString", "false")
      .with("graphqlType", "LocalTime")
      .with("isTime", "true")
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.UPDATE_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.UPDATE_TIME_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("updateFunction", "now")
      .with("javaType", "java.time.LocalDateTime")
      .with("sqlType", "TIMESTAMP")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "false")
      .with("mssql_sqlType", "dateTime")
      .with("isDateTime", "true")
      .with("createFunction", "now")
      .with("isDate", "true")
      .with("isString", "false")
      .with("graphqlType", "LocalTime")
      .with("isTime", "true")
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.checkerfixadvanced.orderitem.OrderItem.VERSION_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      $factory.register(entityDescriptor);
  }
}