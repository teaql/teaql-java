
package com.example.schoolmanagementservice;

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
    registerPlatform();
    registerSchoolType();
    registerSchool();
  }
  private void registerPlatform() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.schoolmanagementservice.platform.Platform.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.schoolmanagementservice.platform.Platform.class);
      entityDescriptor.setEntitySupplier(com.example.schoolmanagementservice.platform.Platform::new);
      entityDescriptor.with("name", "Platform")
      .with("module", "Organization")
      .with("module_key", "organization");

      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.platform.Platform.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.platform.Platform.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor baseUrl = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.platform.Platform.BASE_URL_PROPERTY, String.class)
      ;
      PropertyDescriptor createTime = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.platform.Platform.CREATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor updateTime = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.platform.Platform.UPDATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.platform.Platform.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.schoolmanagementservice.platform.Platform.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.platform.Platform.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.platform.Platform.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.platform.Platform.NAME_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "Campus Learning Platform")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.platform.Platform.BASE_URL_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.platform.Platform.BASE_URL_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "https://campus.example.com")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.platform.Platform.CREATE_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.platform.Platform.CREATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.platform.Platform.UPDATE_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.platform.Platform.UPDATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.platform.Platform.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.platform.Platform.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerSchoolType() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.schoolmanagementservice.schooltype.SchoolType.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.schoolmanagementservice.schooltype.SchoolType.class);
      entityDescriptor.setEntitySupplier(com.example.schoolmanagementservice.schooltype.SchoolType::new);
      entityDescriptor.with("name", "School Type")
      .with("module", "Academics")
      .with("module_key", "academics")
      .with("constant", "true")
      .with("identifier", "code");

      PropertyDescriptor platform = 
      entityDescriptor.addObjectProperty($factory, com.example.schoolmanagementservice.schooltype.SchoolType.PLATFORM_PROPERTY, com.example.schoolmanagementservice.platform.Platform.INTERNAL_TYPE, com.example.schoolmanagementservice.platform.Platform.SCHOOL_TYPE_LIST_PROPERTY, com.example.schoolmanagementservice.platform.Platform.class)
      ;
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.schooltype.SchoolType.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.schooltype.SchoolType.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor code = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.schooltype.SchoolType.CODE_PROPERTY, String.class)
      ;
      PropertyDescriptor displayOrder = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.schooltype.SchoolType.DISPLAY_ORDER_PROPERTY, BigDecimal.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.schooltype.SchoolType.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.schoolmanagementservice.schooltype.SchoolType.PLATFORM_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.schooltype.SchoolType.PLATFORM_PROPERTY).with("candidates", "platform()");

      entityDescriptor.findProperty(com.example.schoolmanagementservice.schooltype.SchoolType.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.schooltype.SchoolType.ID_PROPERTY).with("isPassword", "false")
      .with("isVersion", "false")
      .with("oracle_sqlType", "number(11)")
      .with("javaType", "java.lang.Long")
      .with("candidates", "1001,1002")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.schooltype.SchoolType.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.schooltype.SchoolType.NAME_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "Primary,Secondary")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.schooltype.SchoolType.CODE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.schooltype.SchoolType.CODE_PROPERTY).with("identifier", "true")
      .with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "PRIMARY,SECONDARY")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.schooltype.SchoolType.DISPLAY_ORDER_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.schooltype.SchoolType.DISPLAY_ORDER_PROPERTY).with("isPassword", "false")
      .with("db2_sqlType", "decimal(19,7)")
      .with("isVersion", "false")
      .with("oracle_sqlType", "number(19,7)")
      .with("javaType", "java.math.BigDecimal")
      .with("candidates", "1,2")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.schooltype.SchoolType.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.schooltype.SchoolType.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerSchool() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.schoolmanagementservice.school.School.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.schoolmanagementservice.school.School.class);
      entityDescriptor.setEntitySupplier(com.example.schoolmanagementservice.school.School::new);
      entityDescriptor.with("name", "School")
      .with("module", "Academics")
      .with("module_key", "academics");

      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.school.School.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor platform = 
      entityDescriptor.addObjectProperty($factory, com.example.schoolmanagementservice.school.School.PLATFORM_PROPERTY, com.example.schoolmanagementservice.platform.Platform.INTERNAL_TYPE, com.example.schoolmanagementservice.platform.Platform.SCHOOL_LIST_PROPERTY, com.example.schoolmanagementservice.platform.Platform.class)
      ;
      PropertyDescriptor schoolType = 
      entityDescriptor.addObjectProperty($factory, com.example.schoolmanagementservice.school.School.SCHOOL_TYPE_PROPERTY, com.example.schoolmanagementservice.schooltype.SchoolType.INTERNAL_TYPE, com.example.schoolmanagementservice.schooltype.SchoolType.SCHOOL_LIST_PROPERTY, com.example.schoolmanagementservice.schooltype.SchoolType.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.school.School.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor address = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.school.School.ADDRESS_PROPERTY, String.class)
      ;
      PropertyDescriptor establishedDate = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.school.School.ESTABLISHED_DATE_PROPERTY, LocalDate.class)
      ;
      PropertyDescriptor studentCapacity = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.school.School.STUDENT_CAPACITY_PROPERTY, Integer.class)
      ;
      PropertyDescriptor active = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.school.School.ACTIVE_PROPERTY, Boolean.class)
      ;
      PropertyDescriptor createTime = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.school.School.CREATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor updateTime = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.school.School.UPDATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.schoolmanagementservice.school.School.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.PLATFORM_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.SCHOOL_TYPE_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.NAME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.ADDRESS_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.ADDRESS_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.ESTABLISHED_DATE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.ESTABLISHED_DATE_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.STUDENT_CAPACITY_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.STUDENT_CAPACITY_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.ACTIVE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.ACTIVE_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.CREATE_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.CREATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.UPDATE_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.UPDATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.schoolmanagementservice.school.School.VERSION_PROPERTY).with("isPassword", "false")
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