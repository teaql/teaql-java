
package com.teaql.orderattachmentservice;

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
    registerCommercePlatform();
    registerCustomerOrder();
    registerOrderAttachment();
  }
  private void registerCommercePlatform() {
      FieldLayout.installGenerated(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.class);
      entityDescriptor.setEntitySupplier(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform::new);
      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.NAME_PROPERTY).with("isPassword", "false")

      .with("max", "100")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("candidates", "Order Attachments")

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

      entityDescriptor.findProperty(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerCustomerOrder() {
      FieldLayout.installGenerated(com.teaql.orderattachmentservice.customerorder.CustomerOrder.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.orderattachmentservice.customerorder.CustomerOrder.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.orderattachmentservice.customerorder.CustomerOrder.class);
      entityDescriptor.setEntitySupplier(com.teaql.orderattachmentservice.customerorder.CustomerOrder::new);
      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.customerorder.CustomerOrder.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor orderNumber = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY, String.class)
      ;
      PropertyDescriptor commercePlatform = 
      entityDescriptor.addObjectProperty($factory, com.teaql.orderattachmentservice.customerorder.CustomerOrder.COMMERCE_PLATFORM_PROPERTY, com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.INTERNAL_TYPE, com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY, com.teaql.orderattachmentservice.commerceplatform.CommercePlatform.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.customerorder.CustomerOrder.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.customerorder.CustomerOrder.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.customerorder.CustomerOrder.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.orderattachmentservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.orderattachmentservice.customerorder.CustomerOrder.COMMERCE_PLATFORM_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.orderattachmentservice.customerorder.CustomerOrder.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.customerorder.CustomerOrder.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerOrderAttachment() {
      FieldLayout.installGenerated(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.class);
      entityDescriptor.setEntitySupplier(com.teaql.orderattachmentservice.orderattachment.OrderAttachment::new);
      entityDescriptor.with("audit_mask_fields", "object_key")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.OBJECT_KEY_PROPERTY));
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor customerOrder = 
      entityDescriptor.addObjectProperty($factory, com.teaql.orderattachmentservice.orderattachment.OrderAttachment.CUSTOMER_ORDER_PROPERTY, com.teaql.orderattachmentservice.customerorder.CustomerOrder.INTERNAL_TYPE, com.teaql.orderattachmentservice.customerorder.CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY, com.teaql.orderattachmentservice.customerorder.CustomerOrder.class)
      ;
      PropertyDescriptor fileName = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.FILE_NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor contentType = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.CONTENT_TYPE_PROPERTY, String.class)
      ;
      PropertyDescriptor contentLength = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.CONTENT_LENGTH_PROPERTY, Integer.class)
      ;
      PropertyDescriptor contentSha256 = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.CONTENT_SHA256_PROPERTY, String.class)
      ;
      PropertyDescriptor objectKey = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.OBJECT_KEY_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.CUSTOMER_ORDER_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.FILE_NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.FILE_NAME_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.CONTENT_TYPE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.CONTENT_TYPE_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.CONTENT_LENGTH_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.CONTENT_LENGTH_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.CONTENT_SHA256_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.CONTENT_SHA256_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.OBJECT_KEY_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.OBJECT_KEY_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.orderattachmentservice.orderattachment.OrderAttachment.VERSION_PROPERTY).with("isPassword", "false")

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