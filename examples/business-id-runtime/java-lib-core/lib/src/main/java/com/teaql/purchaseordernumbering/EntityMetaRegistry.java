
package com.teaql.purchaseordernumbering;

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
    registerPlatform();
    registerPurchaseOrder();
  }
  private void registerPlatform() {
      FieldLayout.installGenerated(com.teaql.purchaseordernumbering.platform.Platform.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.purchaseordernumbering.platform.Platform.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.purchaseordernumbering.platform.Platform.class);
      entityDescriptor.setEntitySupplier(com.teaql.purchaseordernumbering.platform.Platform::new);
      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.purchaseordernumbering.platform.Platform.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.purchaseordernumbering.platform.Platform.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.purchaseordernumbering.platform.Platform.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.platform.Platform.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.platform.Platform.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.platform.Platform.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.platform.Platform.NAME_PROPERTY).with("isPassword", "false")

      .with("max", "100")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("candidates", "Procurement")

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

      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.platform.Platform.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.platform.Platform.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerPurchaseOrder() {
      FieldLayout.installGenerated(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.class);
      entityDescriptor.setEntitySupplier(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder::new);
      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor platform = 
      entityDescriptor.addObjectProperty($factory, com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.PLATFORM_PROPERTY, com.teaql.purchaseordernumbering.platform.Platform.INTERNAL_TYPE, com.teaql.purchaseordernumbering.platform.Platform.PURCHASE_ORDER_LIST_PROPERTY, com.teaql.purchaseordernumbering.platform.Platform.class)
      ;
      PropertyDescriptor supplier = 
      entityDescriptor.addSimpleProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.SUPPLIER_PROPERTY, String.class)
      ;
      PropertyDescriptor orderNumber = 
      entityDescriptor.addSimpleProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.ORDER_NUMBER_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.PLATFORM_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.SUPPLIER_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.SUPPLIER_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.ORDER_NUMBER_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.ORDER_NUMBER_PROPERTY).with("policyVersion", "1")

      .with("splitbydate", "true")

      .with("isPassword", "false")

      .with("dateFormat", "yyyyMMdd")

      .with("max", "100")

      .with("prefix", "ORD")

      .with("profile", "daily-permuted-v1")

      .with("preserve_digits", "6")

      .with("isVersion", "false")

      .with("separator", "-")

      .with("javaType", "java.lang.String")

      .with("sqlType", "VARCHAR(<max>)")

      .with("isId", "false")

      .with("isBool", "false")

      .with("isBaseEntityField", "false")

      .with("isNumber", "false")

      .with("namespace", "order_number")

      .with("reset", "daily")

      .with("isString", "true")

      .with("isDate", "false")

      .with("isBusinessId", "true")

      .with("graphqlType", "String")

      .with("isTime", "false")

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.VERSION_PROPERTY).with("isPassword", "false")

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