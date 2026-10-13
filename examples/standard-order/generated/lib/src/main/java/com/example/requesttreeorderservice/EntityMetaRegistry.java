
package com.example.requesttreeorderservice;

import io.teaql.core.FieldLayout;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaAssembler;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import java.math.BigDecimal;
import java.time.LocalDate;

public class EntityMetaRegistry implements EntityMetaAssembler {
  private EntityMetaFactory $factory;

  @Override
  public void assemble(EntityMetaFactory factory) {
    this.$factory = factory;
    registerPlatform();
    registerOrderStatus();
    registerCustomer();
    registerCustomerOrder();
    registerOrderItem();
  }
  private void registerPlatform() {
      FieldLayout.installGenerated(com.example.requesttreeorderservice.platform.Platform.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.requesttreeorderservice.platform.Platform.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.requesttreeorderservice.platform.Platform.class);
      entityDescriptor.setEntitySupplier(com.example.requesttreeorderservice.platform.Platform::new);
      entityDescriptor.with("name", "Commerce Platform")
      .with("module", "Commerce")
      .with("module_key", "commerce");

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.platform.Platform.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.platform.Platform.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.platform.Platform.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.requesttreeorderservice.platform.Platform.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.platform.Platform.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.requesttreeorderservice.platform.Platform.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.platform.Platform.NAME_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "Commerce Test Platform")
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

      entityDescriptor.findProperty(com.example.requesttreeorderservice.platform.Platform.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.platform.Platform.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerOrderStatus() {
      FieldLayout.installGenerated(com.example.requesttreeorderservice.orderstatus.OrderStatus.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.requesttreeorderservice.orderstatus.OrderStatus.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.requesttreeorderservice.orderstatus.OrderStatus.class);
      entityDescriptor.setEntitySupplier(com.example.requesttreeorderservice.orderstatus.OrderStatus::new);
      entityDescriptor.with("name", "Order Status")
      .with("module", "Commerce")
      .with("module_key", "commerce")
      .with("constant", "true")
      .with("identifier", "code");

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor platform = 
      entityDescriptor.addObjectProperty($factory, com.example.requesttreeorderservice.orderstatus.OrderStatus.PLATFORM_PROPERTY, com.example.requesttreeorderservice.platform.Platform.INTERNAL_TYPE, com.example.requesttreeorderservice.platform.Platform.ORDER_STATUS_LIST_PROPERTY, com.example.requesttreeorderservice.platform.Platform.class)
      ;
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor code = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.CODE_PROPERTY, String.class)
      ;
      PropertyDescriptor color = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.COLOR_PROPERTY, String.class)
      ;
      PropertyDescriptor displayOrder = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.DISPLAY_ORDER_PROPERTY, BigDecimal.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.PLATFORM_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.PLATFORM_PROPERTY).with("candidates", "platform()");

      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.ID_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("oracle_sqlType", "number(11)")
      .with("javaType", "java.lang.Long")
      .with("candidates", "1001,1002,1003")
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

      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.NAME_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "New,Paid,Cancelled")
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

      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.CODE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.CODE_PROPERTY).with("identifier", "true")
      .with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "NEW,PAID,CANCELLED")
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

      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.COLOR_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.COLOR_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "#94A3B8,#22C55E,#EF4444")
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

      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.DISPLAY_ORDER_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.DISPLAY_ORDER_PROPERTY).with("isPassword", "false")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("oracle_sqlType", "number(19,7)")
      .with("javaType", "java.math.BigDecimal")
      .with("candidates", "1,2,3")
      .with("sqlType", "NUMERIC(19,7)")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "true")
      .with("isString", "false")
      .with("isDate", "false")
      .with("graphqlType", "BigDecimal")
      .with("isTime", "false")
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderstatus.OrderStatus.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerCustomer() {
      FieldLayout.installGenerated(com.example.requesttreeorderservice.customer.Customer.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.requesttreeorderservice.customer.Customer.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.requesttreeorderservice.customer.Customer.class);
      entityDescriptor.setEntitySupplier(com.example.requesttreeorderservice.customer.Customer::new);
      entityDescriptor.with("name", "Customer")
      .with("module", "Commerce")
      .with("module_key", "commerce")
      .with("audit_mask_fields", "name");

      entityDescriptor.setAuditMaskFields(java.util.List.of(com.example.requesttreeorderservice.customer.Customer.NAME_PROPERTY));
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.customer.Customer.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor platform = 
      entityDescriptor.addObjectProperty($factory, com.example.requesttreeorderservice.customer.Customer.PLATFORM_PROPERTY, com.example.requesttreeorderservice.platform.Platform.INTERNAL_TYPE, com.example.requesttreeorderservice.platform.Platform.CUSTOMER_LIST_PROPERTY, com.example.requesttreeorderservice.platform.Platform.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.customer.Customer.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.customer.Customer.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.requesttreeorderservice.customer.Customer.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.customer.Customer.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.requesttreeorderservice.customer.Customer.PLATFORM_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.requesttreeorderservice.customer.Customer.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.customer.Customer.NAME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.requesttreeorderservice.customer.Customer.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.customer.Customer.VERSION_PROPERTY).with("isPassword", "false")
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
      FieldLayout.installGenerated(com.example.requesttreeorderservice.customerorder.CustomerOrder.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.requesttreeorderservice.customerorder.CustomerOrder.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.requesttreeorderservice.customerorder.CustomerOrder.class);
      entityDescriptor.setEntitySupplier(com.example.requesttreeorderservice.customerorder.CustomerOrder::new);
      entityDescriptor.with("name", "Customer Order")
      .with("module", "Commerce")
      .with("module_key", "commerce")
      .with("audit_mask_fields", "order_number");

      entityDescriptor.setAuditMaskFields(java.util.List.of(com.example.requesttreeorderservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY));
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor customer = 
      entityDescriptor.addObjectProperty($factory, com.example.requesttreeorderservice.customerorder.CustomerOrder.CUSTOMER_PROPERTY, com.example.requesttreeorderservice.customer.Customer.INTERNAL_TYPE, com.example.requesttreeorderservice.customer.Customer.CUSTOMER_ORDER_LIST_PROPERTY, com.example.requesttreeorderservice.customer.Customer.class)
      ;
      PropertyDescriptor status = 
      entityDescriptor.addObjectProperty($factory, com.example.requesttreeorderservice.customerorder.CustomerOrder.STATUS_PROPERTY, com.example.requesttreeorderservice.orderstatus.OrderStatus.INTERNAL_TYPE, com.example.requesttreeorderservice.orderstatus.OrderStatus.CUSTOMER_ORDER_LIST_PROPERTY, com.example.requesttreeorderservice.orderstatus.OrderStatus.class)
      ;
      PropertyDescriptor orderNumber = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY, String.class)
      ;
      PropertyDescriptor orderDate = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.ORDER_DATE_PROPERTY, LocalDate.class)
      ;
      PropertyDescriptor totalAmount = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.TOTAL_AMOUNT_PROPERTY, BigDecimal.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.CUSTOMER_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.STATUS_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.ORDER_DATE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.ORDER_DATE_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("javaType", "java.time.LocalDate")
      .with("sqlType", "DATE")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "false")
      .with("isDate", "true")
      .with("isString", "false")
      .with("graphqlType", "Date")
      .with("isTime", "false")
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.TOTAL_AMOUNT_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.TOTAL_AMOUNT_PROPERTY).with("isPassword", "false")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("oracle_sqlType", "number(19,7)")
      .with("javaType", "java.math.BigDecimal")
      .with("sqlType", "NUMERIC(19,7)")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "true")
      .with("isString", "false")
      .with("isDate", "false")
      .with("graphqlType", "BigDecimal")
      .with("isTime", "false")
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.customerorder.CustomerOrder.VERSION_PROPERTY).with("isPassword", "false")
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
      FieldLayout.installGenerated(com.example.requesttreeorderservice.orderitem.OrderItem.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.requesttreeorderservice.orderitem.OrderItem.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.requesttreeorderservice.orderitem.OrderItem.class);
      entityDescriptor.setEntitySupplier(com.example.requesttreeorderservice.orderitem.OrderItem::new);
      entityDescriptor.with("name", "Order Item")
      .with("module", "Commerce")
      .with("module_key", "commerce");

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.orderitem.OrderItem.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor customerOrder = 
      entityDescriptor.addObjectProperty($factory, com.example.requesttreeorderservice.orderitem.OrderItem.CUSTOMER_ORDER_PROPERTY, com.example.requesttreeorderservice.customerorder.CustomerOrder.INTERNAL_TYPE, com.example.requesttreeorderservice.customerorder.CustomerOrder.ORDER_ITEM_LIST_PROPERTY, com.example.requesttreeorderservice.customerorder.CustomerOrder.class)
      ;
      PropertyDescriptor productName = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.orderitem.OrderItem.PRODUCT_NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor quantity = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.orderitem.OrderItem.QUANTITY_PROPERTY, Integer.class)
      ;
      PropertyDescriptor unitPrice = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.orderitem.OrderItem.UNIT_PRICE_PROPERTY, BigDecimal.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.requesttreeorderservice.orderitem.OrderItem.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderitem.OrderItem.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderitem.OrderItem.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderitem.OrderItem.CUSTOMER_ORDER_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderitem.OrderItem.PRODUCT_NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderitem.OrderItem.PRODUCT_NAME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderitem.OrderItem.QUANTITY_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderitem.OrderItem.QUANTITY_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderitem.OrderItem.UNIT_PRICE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderitem.OrderItem.UNIT_PRICE_PROPERTY).with("isPassword", "false")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("oracle_sqlType", "number(19,7)")
      .with("javaType", "java.math.BigDecimal")
      .with("sqlType", "NUMERIC(19,7)")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "true")
      .with("isString", "false")
      .with("isDate", "false")
      .with("graphqlType", "BigDecimal")
      .with("isTime", "false")
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderitem.OrderItem.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.requesttreeorderservice.orderitem.OrderItem.VERSION_PROPERTY).with("isPassword", "false")
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