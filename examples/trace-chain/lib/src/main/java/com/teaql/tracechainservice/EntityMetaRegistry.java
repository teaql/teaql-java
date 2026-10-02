
package com.teaql.tracechainservice;

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
    registerCustomerOrder();
    registerOrderItem();
    registerPayment();
    registerPaymentAttempt();
    registerShipment();
  }
  private void registerPlatform() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.tracechainservice.platform.Platform.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.tracechainservice.platform.Platform.class);
      entityDescriptor.setEntitySupplier(com.teaql.tracechainservice.platform.Platform::new);
      entityDescriptor.with("name", "Platform")
      .with("module", "Trace Chain")
      .with("module_key", "trace-chain");

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.platform.Platform.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.platform.Platform.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.platform.Platform.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.tracechainservice.platform.Platform.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.platform.Platform.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.tracechainservice.platform.Platform.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.platform.Platform.NAME_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "Trace Chain Verification")
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

      entityDescriptor.findProperty(com.teaql.tracechainservice.platform.Platform.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.platform.Platform.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerCustomerOrder() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.tracechainservice.customerorder.CustomerOrder.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.tracechainservice.customerorder.CustomerOrder.class);
      entityDescriptor.setEntitySupplier(com.teaql.tracechainservice.customerorder.CustomerOrder::new);
      entityDescriptor.with("name", "Customer Order")
      .with("module", "Trace Chain")
      .with("module_key", "trace-chain");

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor platform = 
      entityDescriptor.addObjectProperty($factory, com.teaql.tracechainservice.customerorder.CustomerOrder.PLATFORM_PROPERTY, com.teaql.tracechainservice.platform.Platform.INTERNAL_TYPE, com.teaql.tracechainservice.platform.Platform.CUSTOMER_ORDER_LIST_PROPERTY, com.teaql.tracechainservice.platform.Platform.class)
      ;
      PropertyDescriptor orderNumber = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY, String.class)
      ;
      PropertyDescriptor description = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.DESCRIPTION_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.PLATFORM_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.DESCRIPTION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.DESCRIPTION_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.customerorder.CustomerOrder.VERSION_PROPERTY).with("isPassword", "false")
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
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.tracechainservice.orderitem.OrderItem.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.tracechainservice.orderitem.OrderItem.class);
      entityDescriptor.setEntitySupplier(com.teaql.tracechainservice.orderitem.OrderItem::new);
      entityDescriptor.with("name", "Order Item")
      .with("module", "Trace Chain")
      .with("module_key", "trace-chain");

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.orderitem.OrderItem.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor customerOrder = 
      entityDescriptor.addObjectProperty($factory, com.teaql.tracechainservice.orderitem.OrderItem.CUSTOMER_ORDER_PROPERTY, com.teaql.tracechainservice.customerorder.CustomerOrder.INTERNAL_TYPE, com.teaql.tracechainservice.customerorder.CustomerOrder.ORDER_ITEM_LIST_PROPERTY, com.teaql.tracechainservice.customerorder.CustomerOrder.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.orderitem.OrderItem.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.orderitem.OrderItem.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.tracechainservice.orderitem.OrderItem.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.orderitem.OrderItem.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.tracechainservice.orderitem.OrderItem.CUSTOMER_ORDER_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.tracechainservice.orderitem.OrderItem.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.orderitem.OrderItem.NAME_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.tracechainservice.orderitem.OrderItem.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.orderitem.OrderItem.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerPayment() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.tracechainservice.payment.Payment.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.tracechainservice.payment.Payment.class);
      entityDescriptor.setEntitySupplier(com.teaql.tracechainservice.payment.Payment::new);
      entityDescriptor.with("name", "Payment")
      .with("module", "Trace Chain")
      .with("module_key", "trace-chain");

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.payment.Payment.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor customerOrder = 
      entityDescriptor.addObjectProperty($factory, com.teaql.tracechainservice.payment.Payment.CUSTOMER_ORDER_PROPERTY, com.teaql.tracechainservice.customerorder.CustomerOrder.INTERNAL_TYPE, com.teaql.tracechainservice.customerorder.CustomerOrder.PAYMENT_LIST_PROPERTY, com.teaql.tracechainservice.customerorder.CustomerOrder.class)
      ;
      PropertyDescriptor referenceCode = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.payment.Payment.REFERENCE_CODE_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.payment.Payment.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.tracechainservice.payment.Payment.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.payment.Payment.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.tracechainservice.payment.Payment.CUSTOMER_ORDER_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.tracechainservice.payment.Payment.REFERENCE_CODE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.payment.Payment.REFERENCE_CODE_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.tracechainservice.payment.Payment.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.payment.Payment.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerPaymentAttempt() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.class);
      entityDescriptor.setEntitySupplier(com.teaql.tracechainservice.paymentattempt.PaymentAttempt::new);
      entityDescriptor.with("name", "Payment Attempt")
      .with("module", "Trace Chain")
      .with("module_key", "trace-chain");

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor payment = 
      entityDescriptor.addObjectProperty($factory, com.teaql.tracechainservice.paymentattempt.PaymentAttempt.PAYMENT_PROPERTY, com.teaql.tracechainservice.payment.Payment.INTERNAL_TYPE, com.teaql.tracechainservice.payment.Payment.PAYMENT_ATTEMPT_LIST_PROPERTY, com.teaql.tracechainservice.payment.Payment.class)
      ;
      PropertyDescriptor referenceCode = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.REFERENCE_CODE_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.PAYMENT_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.REFERENCE_CODE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.REFERENCE_CODE_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.paymentattempt.PaymentAttempt.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerShipment() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.tracechainservice.shipment.Shipment.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.tracechainservice.shipment.Shipment.class);
      entityDescriptor.setEntitySupplier(com.teaql.tracechainservice.shipment.Shipment::new);
      entityDescriptor.with("name", "Shipment")
      .with("module", "Trace Chain")
      .with("module_key", "trace-chain");

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.shipment.Shipment.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor customerOrder = 
      entityDescriptor.addObjectProperty($factory, com.teaql.tracechainservice.shipment.Shipment.CUSTOMER_ORDER_PROPERTY, com.teaql.tracechainservice.customerorder.CustomerOrder.INTERNAL_TYPE, com.teaql.tracechainservice.customerorder.CustomerOrder.SHIPMENT_LIST_PROPERTY, com.teaql.tracechainservice.customerorder.CustomerOrder.class)
      ;
      PropertyDescriptor referenceCode = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.shipment.Shipment.REFERENCE_CODE_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.tracechainservice.shipment.Shipment.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.tracechainservice.shipment.Shipment.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.shipment.Shipment.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.tracechainservice.shipment.Shipment.CUSTOMER_ORDER_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.tracechainservice.shipment.Shipment.REFERENCE_CODE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.shipment.Shipment.REFERENCE_CODE_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.tracechainservice.shipment.Shipment.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.tracechainservice.shipment.Shipment.VERSION_PROPERTY).with("isPassword", "false")
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