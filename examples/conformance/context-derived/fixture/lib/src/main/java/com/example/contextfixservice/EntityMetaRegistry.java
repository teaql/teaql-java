
package com.example.contextfixservice;

import io.teaql.core.FieldLayout;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaAssembler;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import java.time.LocalDateTime;

public class EntityMetaRegistry implements EntityMetaAssembler {
  private EntityMetaFactory $factory;

  @Override
  public void assemble(EntityMetaFactory factory) {
    this.$factory = factory;
    registerCatalog();
    registerEntry();
  }
  private void registerCatalog() {
      FieldLayout.installGenerated(com.example.contextfixservice.catalog.Catalog.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.contextfixservice.catalog.Catalog.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.contextfixservice.catalog.Catalog.class);
      entityDescriptor.setEntitySupplier(com.example.contextfixservice.catalog.Catalog::new);
      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.catalog.Catalog.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.catalog.Catalog.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.catalog.Catalog.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.contextfixservice.catalog.Catalog.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.catalog.Catalog.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.contextfixservice.catalog.Catalog.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.catalog.Catalog.NAME_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "Catalog")
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

      entityDescriptor.findProperty(com.example.contextfixservice.catalog.Catalog.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.catalog.Catalog.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerEntry() {
      FieldLayout.installGenerated(com.example.contextfixservice.entry.Entry.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.contextfixservice.entry.Entry.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.contextfixservice.entry.Entry.class);
      entityDescriptor.setEntitySupplier(com.example.contextfixservice.entry.Entry::new);
      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.entry.Entry.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor catalog = 
      entityDescriptor.addObjectProperty($factory, com.example.contextfixservice.entry.Entry.CATALOG_PROPERTY, com.example.contextfixservice.catalog.Catalog.INTERNAL_TYPE, com.example.contextfixservice.catalog.Catalog.ENTRY_LIST_PROPERTY, com.example.contextfixservice.catalog.Catalog.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.entry.Entry.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor bornAt = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.entry.Entry.BORN_AT_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor touchedAt = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.entry.Entry.TOUCHED_AT_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor sourceIp = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.entry.Entry.SOURCE_IP_PROPERTY, String.class)
      ;
      PropertyDescriptor lastIp = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.entry.Entry.LAST_IP_PROPERTY, String.class)
      ;
      PropertyDescriptor openedBy = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.entry.Entry.OPENED_BY_PROPERTY, String.class)
      ;
      PropertyDescriptor changedBy = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.entry.Entry.CHANGED_BY_PROPERTY, String.class)
      ;
      PropertyDescriptor guardedToken = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.entry.Entry.GUARDED_TOKEN_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.contextfixservice.entry.Entry.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.CATALOG_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.NAME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.BORN_AT_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.BORN_AT_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.TOUCHED_AT_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.TOUCHED_AT_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.SOURCE_IP_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.SOURCE_IP_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.LAST_IP_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.LAST_IP_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.OPENED_BY_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.OPENED_BY_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.CHANGED_BY_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.CHANGED_BY_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.GUARDED_TOKEN_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.GUARDED_TOKEN_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("prefix", "")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("sqlType", "VARCHAR(<max>)")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "false")
      .with("createFunction", "uniStr")
      .with("isString", "true")
      .with("isDate", "false")
      .with("graphqlType", "String")
      .with("isTime", "false")
      .with("isText", "false")
      .with("createFunctionArgNames", "prefix");

      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.contextfixservice.entry.Entry.VERSION_PROPERTY).with("isPassword", "false")
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