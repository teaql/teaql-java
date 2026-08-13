
package com.teaql.ordermanagementservice;

import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaAssembler;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EntityMetaRegistry implements EntityMetaAssembler {
  private EntityMetaFactory $factory;

  @Override
  public void assemble(EntityMetaFactory factory) {
    this.$factory = factory;
    registerCommercePlatform();
    registerCustomer();
    registerOrderStatus();
    registerCustomerOrder();
    registerProduct();
    registerOrderLine();
    registerOrderSearchPreset();
  }
  private void registerCommercePlatform() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.class);
      entityDescriptor.setEntitySupplier(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform::new);
      entityDescriptor.with("name", "Commerce Platform")
      .with("module", "Platform")
      .with("module_key", "platform");

      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor createTime = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.CREATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor updateTime = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.UPDATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.NAME_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "Northwind Demo")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.CREATE_TIME_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("javaType", "java.time.LocalDateTime")
      .with("candidates", "createTime()")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.UPDATE_TIME_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("updateFunction", "now")
      .with("javaType", "java.time.LocalDateTime")
      .with("candidates", "updateTime()")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.VERSION_PROPERTY).with("isPassword", "false")
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
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.ordermanagementservice.customer.Customer.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.ordermanagementservice.customer.Customer.class);
      entityDescriptor.setEntitySupplier(com.teaql.ordermanagementservice.customer.Customer::new);
      entityDescriptor.with("audit_mask_fields", "name,email")
      .with("name", "Customer")
      .with("module", "Commerce")
      .with("module_key", "commerce");

      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customer.Customer.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customer.Customer.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor email = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customer.Customer.EMAIL_PROPERTY, String.class)
      ;
      PropertyDescriptor commercePlatform = 
      entityDescriptor.addObjectProperty($factory, com.teaql.ordermanagementservice.customer.Customer.COMMERCE_PLATFORM_PROPERTY, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.INTERNAL_TYPE, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.CUSTOMER_LIST_PROPERTY, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.class)
      ;
      PropertyDescriptor createTime = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customer.Customer.CREATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor updateTime = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customer.Customer.UPDATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customer.Customer.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customer.Customer.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customer.Customer.NAME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customer.Customer.EMAIL_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customer.Customer.COMMERCE_PLATFORM_PROPERTY).with("context", "true");

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customer.Customer.CREATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customer.Customer.UPDATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customer.Customer.VERSION_PROPERTY).with("isPassword", "false")
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
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.ordermanagementservice.orderstatus.OrderStatus.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.ordermanagementservice.orderstatus.OrderStatus.class);
      entityDescriptor.setEntitySupplier(com.teaql.ordermanagementservice.orderstatus.OrderStatus::new);
      entityDescriptor.with("features", "status")
      .with("identifier", "code")
      .with("constant", "true")
      .with("name", "Order Status")
      .with("module", "Order")
      .with("module_key", "order");

      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor code = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.CODE_PROPERTY, String.class)
      ;
      PropertyDescriptor color = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.COLOR_PROPERTY, String.class)
      ;
      PropertyDescriptor displayOrder = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.DISPLAY_ORDER_PROPERTY, BigDecimal.class)
      ;
      PropertyDescriptor commercePlatform = 
      entityDescriptor.addObjectProperty($factory, com.teaql.ordermanagementservice.orderstatus.OrderStatus.COMMERCE_PLATFORM_PROPERTY, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.INTERNAL_TYPE, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.ORDER_STATUS_LIST_PROPERTY, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.ID_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("oracle_sqlType", "number(11)")
      .with("javaType", "java.lang.Long")
      .with("candidates", "1001,1002,1003,1004")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.NAME_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "Pending,Processing,Shipped,Completed")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.CODE_PROPERTY).with("identifier", "true")
      .with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "PENDING,PROCESSING,SHIPPED,COMPLETED")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.COLOR_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "#F97316,#2563EB,#16A34A,#334155")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.DISPLAY_ORDER_PROPERTY).with("isPassword", "false")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("oracle_sqlType", "number(19,7)")
      .with("javaType", "java.math.BigDecimal")
      .with("candidates", "10,20,30,40")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.COMMERCE_PLATFORM_PROPERTY).with("candidates", "commerce_platform()");

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderstatus.OrderStatus.VERSION_PROPERTY).with("isPassword", "false")
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
      entityDescriptor.setType(com.teaql.ordermanagementservice.customerorder.CustomerOrder.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.ordermanagementservice.customerorder.CustomerOrder.class);
      entityDescriptor.setEntitySupplier(com.teaql.ordermanagementservice.customerorder.CustomerOrder::new);
      entityDescriptor.with("name", "Customer Order")
      .with("module", "Order")
      .with("module_key", "order");

      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor orderNumber = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY, String.class)
      ;
      PropertyDescriptor orderDate = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.ORDER_DATE_PROPERTY, LocalDate.class)
      ;
      PropertyDescriptor totalAmount = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.TOTAL_AMOUNT_PROPERTY, BigDecimal.class)
      ;
      PropertyDescriptor status = 
      entityDescriptor.addObjectProperty($factory, com.teaql.ordermanagementservice.customerorder.CustomerOrder.STATUS_PROPERTY, com.teaql.ordermanagementservice.orderstatus.OrderStatus.INTERNAL_TYPE, com.teaql.ordermanagementservice.orderstatus.OrderStatus.CUSTOMER_ORDER_LIST_PROPERTY, com.teaql.ordermanagementservice.orderstatus.OrderStatus.class)
      ;
      PropertyDescriptor customer = 
      entityDescriptor.addObjectProperty($factory, com.teaql.ordermanagementservice.customerorder.CustomerOrder.CUSTOMER_PROPERTY, com.teaql.ordermanagementservice.customer.Customer.INTERNAL_TYPE, com.teaql.ordermanagementservice.customer.Customer.CUSTOMER_ORDER_LIST_PROPERTY, com.teaql.ordermanagementservice.customer.Customer.class)
      ;
      PropertyDescriptor commercePlatform = 
      entityDescriptor.addObjectProperty($factory, com.teaql.ordermanagementservice.customerorder.CustomerOrder.COMMERCE_PLATFORM_PROPERTY, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.INTERNAL_TYPE, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.class)
      ;
      PropertyDescriptor createTime = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.CREATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor updateTime = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.UPDATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.ORDER_DATE_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.TOTAL_AMOUNT_PROPERTY).with("isPassword", "false")
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



      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.COMMERCE_PLATFORM_PROPERTY).with("context", "true");

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.CREATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.UPDATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.customerorder.CustomerOrder.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerProduct() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.ordermanagementservice.product.Product.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.ordermanagementservice.product.Product.class);
      entityDescriptor.setEntitySupplier(com.teaql.ordermanagementservice.product.Product::new);
      entityDescriptor.with("name", "Product")
      .with("module", "Commerce")
      .with("module_key", "commerce");

      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.product.Product.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.product.Product.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor sku = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.product.Product.SKU_PROPERTY, String.class)
      ;
      PropertyDescriptor imageUrl = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.product.Product.IMAGE_URL_PROPERTY, String.class)
      ;
      PropertyDescriptor commercePlatform = 
      entityDescriptor.addObjectProperty($factory, com.teaql.ordermanagementservice.product.Product.COMMERCE_PLATFORM_PROPERTY, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.INTERNAL_TYPE, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.PRODUCT_LIST_PROPERTY, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.class)
      ;
      PropertyDescriptor createTime = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.product.Product.CREATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor updateTime = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.product.Product.UPDATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.product.Product.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.ordermanagementservice.product.Product.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.product.Product.NAME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.product.Product.SKU_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.product.Product.IMAGE_URL_PROPERTY).with("isPassword", "false")
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
      .with("zh_CN", "https")
      .with("graphqlType", "String")
      .with("isTime", "false")
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.product.Product.COMMERCE_PLATFORM_PROPERTY).with("context", "true");

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.product.Product.CREATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.product.Product.UPDATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.product.Product.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerOrderLine() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.ordermanagementservice.orderline.OrderLine.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.ordermanagementservice.orderline.OrderLine.class);
      entityDescriptor.setEntitySupplier(com.teaql.ordermanagementservice.orderline.OrderLine::new);
      entityDescriptor.with("name", "Order Line")
      .with("module", "Order")
      .with("module_key", "order");

      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.orderline.OrderLine.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor customerOrder = 
      entityDescriptor.addObjectProperty($factory, com.teaql.ordermanagementservice.orderline.OrderLine.CUSTOMER_ORDER_PROPERTY, com.teaql.ordermanagementservice.customerorder.CustomerOrder.INTERNAL_TYPE, com.teaql.ordermanagementservice.customerorder.CustomerOrder.ORDER_LINE_LIST_PROPERTY, com.teaql.ordermanagementservice.customerorder.CustomerOrder.class)
      ;
      PropertyDescriptor product = 
      entityDescriptor.addObjectProperty($factory, com.teaql.ordermanagementservice.orderline.OrderLine.PRODUCT_PROPERTY, com.teaql.ordermanagementservice.product.Product.INTERNAL_TYPE, com.teaql.ordermanagementservice.product.Product.ORDER_LINE_LIST_PROPERTY, com.teaql.ordermanagementservice.product.Product.class)
      ;
      PropertyDescriptor productName = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.orderline.OrderLine.PRODUCT_NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor sku = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.orderline.OrderLine.SKU_PROPERTY, String.class)
      ;
      PropertyDescriptor quantity = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.orderline.OrderLine.QUANTITY_PROPERTY, Integer.class)
      ;
      PropertyDescriptor commercePlatform = 
      entityDescriptor.addObjectProperty($factory, com.teaql.ordermanagementservice.orderline.OrderLine.COMMERCE_PLATFORM_PROPERTY, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.INTERNAL_TYPE, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.ORDER_LINE_LIST_PROPERTY, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.class)
      ;
      PropertyDescriptor createTime = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.orderline.OrderLine.CREATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.orderline.OrderLine.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderline.OrderLine.ID_PROPERTY).with("isPassword", "false")
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



      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderline.OrderLine.PRODUCT_NAME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderline.OrderLine.SKU_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderline.OrderLine.QUANTITY_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderline.OrderLine.COMMERCE_PLATFORM_PROPERTY).with("context", "true");

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderline.OrderLine.CREATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.orderline.OrderLine.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerOrderSearchPreset() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.class);
      entityDescriptor.setEntitySupplier(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset::new);
      entityDescriptor.with("audit_mask_fields", "filter_json")
      .with("name", "Order Search Preset")
      .with("module", "Order")
      .with("module_key", "order");

      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor filterJson = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.FILTER_JSON_PROPERTY, String.class)
      ;
      PropertyDescriptor requestId = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.REQUEST_ID_PROPERTY, String.class)
      ;
      PropertyDescriptor ownerUserId = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.OWNER_USER_ID_PROPERTY, String.class)
      ;
      PropertyDescriptor commercePlatform = 
      entityDescriptor.addObjectProperty($factory, com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.INTERNAL_TYPE, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.ORDER_SEARCH_PRESET_LIST_PROPERTY, com.teaql.ordermanagementservice.commerceplatform.CommercePlatform.class)
      ;
      PropertyDescriptor createTime = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.CREATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor updateTime = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.UPDATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.NAME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.FILTER_JSON_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.REQUEST_ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.OWNER_USER_ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY).with("context", "true");

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.CREATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.UPDATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset.VERSION_PROPERTY).with("isPassword", "false")
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