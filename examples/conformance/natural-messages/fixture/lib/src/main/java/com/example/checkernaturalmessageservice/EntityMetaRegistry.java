
package com.example.checkernaturalmessageservice;

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
      FieldLayout.installGenerated(com.example.checkernaturalmessageservice.catalog.Catalog.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.checkernaturalmessageservice.catalog.Catalog.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.checkernaturalmessageservice.catalog.Catalog.class);
      entityDescriptor.setEntitySupplier(com.example.checkernaturalmessageservice.catalog.Catalog::new);
      entityDescriptor.with("name", "Validation Catalog")
      .with("module", "Validation")
      .with("module_key", "main");

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.checkernaturalmessageservice.catalog.Catalog.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.checkernaturalmessageservice.catalog.Catalog.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.checkernaturalmessageservice.catalog.Catalog.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.catalog.Catalog.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.catalog.Catalog.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.catalog.Catalog.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.catalog.Catalog.NAME_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "Validation Catalog")
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

      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.catalog.Catalog.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.catalog.Catalog.VERSION_PROPERTY).with("isPassword", "false")
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
      FieldLayout.installGenerated(com.example.checkernaturalmessageservice.entry.Entry.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.checkernaturalmessageservice.entry.Entry.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.checkernaturalmessageservice.entry.Entry.class);
      entityDescriptor.setEntitySupplier(com.example.checkernaturalmessageservice.entry.Entry::new);
      entityDescriptor.with("name", "Validation Entry")
      .with("module", "Validation")
      .with("module_key", "main")
      .with("audit_mask_fields", "protected_note");

      entityDescriptor.setAuditMaskFields(java.util.List.of(com.example.checkernaturalmessageservice.entry.Entry.PROTECTED_NOTE_PROPERTY));
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.checkernaturalmessageservice.entry.Entry.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor catalog = 
      entityDescriptor.addObjectProperty($factory, com.example.checkernaturalmessageservice.entry.Entry.CATALOG_PROPERTY, com.example.checkernaturalmessageservice.catalog.Catalog.INTERNAL_TYPE, com.example.checkernaturalmessageservice.catalog.Catalog.ENTRY_LIST_PROPERTY, com.example.checkernaturalmessageservice.catalog.Catalog.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.checkernaturalmessageservice.entry.Entry.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor quantity = 
      entityDescriptor.addSimpleProperty(com.example.checkernaturalmessageservice.entry.Entry.QUANTITY_PROPERTY, Integer.class)
      ;
      PropertyDescriptor description = 
      entityDescriptor.addSimpleProperty(com.example.checkernaturalmessageservice.entry.Entry.DESCRIPTION_PROPERTY, String.class)
      ;
      PropertyDescriptor protectedNote = 
      entityDescriptor.addSimpleProperty(com.example.checkernaturalmessageservice.entry.Entry.PROTECTED_NOTE_PROPERTY, String.class)
      ;
      PropertyDescriptor createTime = 
      entityDescriptor.addSimpleProperty(com.example.checkernaturalmessageservice.entry.Entry.CREATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor updateTime = 
      entityDescriptor.addSimpleProperty(com.example.checkernaturalmessageservice.entry.Entry.UPDATE_TIME_PROPERTY, LocalDateTime.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.checkernaturalmessageservice.entry.Entry.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.CATALOG_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.NAME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.QUANTITY_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.QUANTITY_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.DESCRIPTION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.DESCRIPTION_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.PROTECTED_NOTE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.PROTECTED_NOTE_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.CREATE_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.CREATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.UPDATE_TIME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.UPDATE_TIME_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.checkernaturalmessageservice.entry.Entry.VERSION_PROPERTY).with("isPassword", "false")
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