
package com.example.freightquotesettlement;

import io.teaql.core.FieldLayout;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaAssembler;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class EntityMetaRegistry implements EntityMetaAssembler {
  private EntityMetaFactory $factory;

  @Override
  public void assemble(EntityMetaFactory factory) {
    this.$factory = factory;
    registerPlatform();
    registerFreightQuote();
  }
  private void registerPlatform() {
      FieldLayout.installGenerated(com.example.freightquotesettlement.platform.Platform.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.freightquotesettlement.platform.Platform.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.freightquotesettlement.platform.Platform.class);
      entityDescriptor.setEntitySupplier(com.example.freightquotesettlement.platform.Platform::new);
      entityDescriptor.with("name", "Dispatch Platform")

      .with("module", "Freight")

      .with("module_key", "freight")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.platform.Platform.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.platform.Platform.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.platform.Platform.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.freightquotesettlement.platform.Platform.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.freightquotesettlement.platform.Platform.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.freightquotesettlement.platform.Platform.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.freightquotesettlement.platform.Platform.NAME_PROPERTY).with("isPassword", "false")

      .with("max", "100")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("candidates", "Harbor Freight Desk")

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

      entityDescriptor.findProperty(com.example.freightquotesettlement.platform.Platform.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.freightquotesettlement.platform.Platform.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerFreightQuote() {
      FieldLayout.installGenerated(com.example.freightquotesettlement.freightquote.FreightQuote.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.freightquotesettlement.freightquote.FreightQuote.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.freightquotesettlement.freightquote.FreightQuote.class);
      entityDescriptor.setEntitySupplier(com.example.freightquotesettlement.freightquote.FreightQuote::new);
      entityDescriptor.with("name", "Freight Quote")

      .with("module", "Freight")

      .with("module_key", "freight")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.freightquote.FreightQuote.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor platform = 
      entityDescriptor.addObjectProperty($factory, com.example.freightquotesettlement.freightquote.FreightQuote.PLATFORM_PROPERTY, com.example.freightquotesettlement.platform.Platform.INTERNAL_TYPE, com.example.freightquotesettlement.platform.Platform.FREIGHT_QUOTE_LIST_PROPERTY, com.example.freightquotesettlement.platform.Platform.class)
      ;
      PropertyDescriptor reference = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.freightquote.FreightQuote.REFERENCE_PROPERTY, String.class)
      ;
      PropertyDescriptor amount = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.freightquote.FreightQuote.AMOUNT_PROPERTY, BigDecimal.class)
      ;
      PropertyDescriptor shipmentNumber = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.freightquote.FreightQuote.SHIPMENT_NUMBER_PROPERTY, Long.class)
      ;
      PropertyDescriptor packageCount = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.freightquote.FreightQuote.PACKAGE_COUNT_PROPERTY, Integer.class)
      ;
      PropertyDescriptor deliveryDate = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.freightquote.FreightQuote.DELIVERY_DATE_PROPERTY, LocalDate.class)
      ;
      PropertyDescriptor pickupTime = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.freightquote.FreightQuote.PICKUP_TIME_PROPERTY, LocalTime.class)
      ;
      PropertyDescriptor quotedAt = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.freightquote.FreightQuote.QUOTED_AT_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor instructions = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.freightquote.FreightQuote.INSTRUCTIONS_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.freightquotesettlement.freightquote.FreightQuote.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.PLATFORM_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.REFERENCE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.REFERENCE_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.AMOUNT_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.AMOUNT_PROPERTY).with("isPassword", "false")

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

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.SHIPMENT_NUMBER_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.SHIPMENT_NUMBER_PROPERTY).with("isPassword", "false")

      .with("db2_sqlType", "decimal(19,7)")

      .with("isVersion", "false")

      .with("oracle_sqlType", "number(11)")

      .with("javaType", "java.lang.Long")

      .with("sqlType", "BIGINT")

      .with("isId", "false")

      .with("isBool", "false")

      .with("isBaseEntityField", "false")

      .with("isNumber", "true")

      .with("isString", "false")

      .with("isDate", "false")

      .with("snowflake_sqlType", "number")

      .with("graphqlType", "Long")

      .with("isTime", "false")

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.PACKAGE_COUNT_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.PACKAGE_COUNT_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.DELIVERY_DATE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.DELIVERY_DATE_PROPERTY).with("isPassword", "false")

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

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.PICKUP_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.PICKUP_TIME_PROPERTY).with("isPassword", "false")

      .with("isVersion", "false")

      .with("javaType", "java.time.LocalTime")

      .with("sqlType", "TIME")

      .with("isId", "false")

      .with("isBool", "false")

      .with("isBaseEntityField", "false")

      .with("isNumber", "false")

      .with("isString", "false")

      .with("isDate", "false")

      .with("graphqlType", "Time")

      .with("isTime", "true")

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.QUOTED_AT_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.QUOTED_AT_PROPERTY).with("isPassword", "false")

      .with("isVersion", "false")

      .with("javaType", "java.time.LocalDateTime")

      .with("sqlType", "TIMESTAMP")

      .with("isId", "false")

      .with("isBool", "false")

      .with("isBaseEntityField", "false")

      .with("isNumber", "false")

      .with("mssql_sqlType", "dateTime")

      .with("isDateTime", "true")

      .with("isDate", "true")

      .with("isString", "false")

      .with("graphqlType", "LocalTime")

      .with("isTime", "true")

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.INSTRUCTIONS_PROPERTY).with("isPassword", "false")

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

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.freightquotesettlement.freightquote.FreightQuote.VERSION_PROPERTY).with("isPassword", "false")

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