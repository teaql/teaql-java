
package com.example.scalartypeconformanceservice;

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
    registerTypePlatform();
    registerScalarTypeProbe();
  }
  private void registerTypePlatform() {
      FieldLayout.installGenerated(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.class);
      entityDescriptor.setEntitySupplier(com.example.scalartypeconformanceservice.typeplatform.TypePlatform::new);
      entityDescriptor.with("name", "Type Platform")
      .with("module", "Type Conformance")
      .with("module_key", "type-conformance");

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.NAME_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "Scalar Type Conformance")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerScalarTypeProbe() {
      FieldLayout.installGenerated(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.class);
      entityDescriptor.setEntitySupplier(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe::new);
      entityDescriptor.with("name", "Scalar Type Probe")
      .with("module", "Type Conformance")
      .with("module_key", "type-conformance");

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor typePlatform = 
      entityDescriptor.addObjectProperty($factory, com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, com.example.scalartypeconformanceservice.typeplatform.TypePlatform.INTERNAL_TYPE, com.example.scalartypeconformanceservice.typeplatform.TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY, com.example.scalartypeconformanceservice.typeplatform.TypePlatform.class)
      ;
      PropertyDescriptor requiredString = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_STRING_PROPERTY, String.class)
      ;
      PropertyDescriptor optionalString = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_STRING_PROPERTY, String.class)
      ;
      PropertyDescriptor largeText = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.LARGE_TEXT_PROPERTY, String.class)
      ;
      PropertyDescriptor requiredBoolean = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY, Boolean.class)
      ;
      PropertyDescriptor optionalBoolean = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_BOOLEAN_PROPERTY, Boolean.class)
      ;
      PropertyDescriptor requiredInteger = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY, Integer.class)
      ;
      PropertyDescriptor optionalInteger = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY, Integer.class)
      ;
      PropertyDescriptor requiredLong = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_LONG_PROPERTY, Long.class)
      ;
      PropertyDescriptor optionalLong = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_LONG_PROPERTY, Long.class)
      ;
      PropertyDescriptor requiredDecimal = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY, BigDecimal.class)
      ;
      PropertyDescriptor optionalDecimal = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY, BigDecimal.class)
      ;
      PropertyDescriptor requiredFloat = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY, Float.class)
      ;
      PropertyDescriptor optionalFloat = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY, Float.class)
      ;
      PropertyDescriptor requiredDouble = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY, Double.class)
      ;
      PropertyDescriptor optionalDouble = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY, Double.class)
      ;
      PropertyDescriptor requiredDate = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_DATE_PROPERTY, LocalDate.class)
      ;
      PropertyDescriptor optionalDate = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_DATE_PROPERTY, LocalDate.class)
      ;
      PropertyDescriptor requiredTime = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_TIME_PROPERTY, LocalTime.class)
      ;
      PropertyDescriptor optionalTime = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_TIME_PROPERTY, LocalTime.class)
      ;
      PropertyDescriptor requiredDatetime = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor optionalDatetime = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_DATETIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor createTime = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.CREATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor updateTime = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.UPDATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.TYPE_PLATFORM_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_STRING_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_STRING_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_STRING_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.LARGE_TEXT_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("optional", "true")
      .with("javaType", "java.lang.String")
      .with("sqlType", "LARGE_TEXT")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "false")
      .with("isString", "true")
      .with("isDate", "false")
      .with("graphqlType", "String")
      .with("isText", "true")
      .with("isTime", "false");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("oracle_sqlType", "number(1)")
      .with("javaType", "java.lang.Boolean")
      .with("sqlType", "BOOLEAN")
      .with("isId", "false")
      .with("isBool", "true")
      .with("isBaseEntityField", "false")
      .with("isNumber", "false")
      .with("mssql_sqlType", "bit")
      .with("isString", "false")
      .with("isDate", "false")
      .with("graphqlType", "Boolean")
      .with("isTime", "false")
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_BOOLEAN_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("optional", "true")
      .with("oracle_sqlType", "number(1)")
      .with("javaType", "java.lang.Boolean")
      .with("sqlType", "BOOLEAN")
      .with("isId", "false")
      .with("isBool", "true")
      .with("isBaseEntityField", "false")
      .with("isNumber", "false")
      .with("mssql_sqlType", "bit")
      .with("isString", "false")
      .with("isDate", "false")
      .with("graphqlType", "Boolean")
      .with("isTime", "false")
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY).with("isPassword", "false")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("optional", "true")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_LONG_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_LONG_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_LONG_PROPERTY).with("isPassword", "false")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("optional", "true")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY).with("isPassword", "false")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("optional", "true")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY).with("isPassword", "false")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("isFloat", "true")
      .with("oracle_sqlType", "binary_float")
      .with("javaType", "java.lang.Float")
      .with("sqlType", "REAL")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "true")
      .with("isString", "false")
      .with("isDate", "false")
      .with("graphqlType", "Float")
      .with("isTime", "false")
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY).with("isPassword", "false")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("optional", "true")
      .with("isFloat", "true")
      .with("oracle_sqlType", "binary_float")
      .with("javaType", "java.lang.Float")
      .with("sqlType", "REAL")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "true")
      .with("isString", "false")
      .with("isDate", "false")
      .with("graphqlType", "Float")
      .with("isTime", "false")
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY).with("isPassword", "false")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("oracle_sqlType", "binary_double")
      .with("javaType", "java.lang.Double")
      .with("sqlType", "DOUBLE PRECISION")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "true")
      .with("mssql_sqlType", "FLOAT")
      .with("isString", "false")
      .with("isDate", "false")
      .with("mysql_sqlType", "DOUBLE")
      .with("graphqlType", "Float")
      .with("isTime", "false")
      .with("isText", "false")
      .with("isDouble", "true");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY).with("isPassword", "false")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("optional", "true")
      .with("oracle_sqlType", "binary_double")
      .with("javaType", "java.lang.Double")
      .with("sqlType", "DOUBLE PRECISION")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "true")
      .with("mssql_sqlType", "FLOAT")
      .with("isString", "false")
      .with("isDate", "false")
      .with("mysql_sqlType", "DOUBLE")
      .with("graphqlType", "Float")
      .with("isTime", "false")
      .with("isText", "false")
      .with("isDouble", "true");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_DATE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_DATE_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_DATE_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("optional", "true")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_TIME_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_TIME_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("optional", "true")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY).with("isPassword", "false")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.OPTIONAL_DATETIME_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("optional", "true")
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
      .with("isText", "false");

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.CREATE_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.CREATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.UPDATE_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.UPDATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.VERSION_PROPERTY).with("isPassword", "false")
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