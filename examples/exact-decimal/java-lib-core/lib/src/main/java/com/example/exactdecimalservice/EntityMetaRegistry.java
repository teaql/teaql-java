
package com.example.exactdecimalservice;

import io.teaql.core.FieldLayout;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaAssembler;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import java.math.BigDecimal;

public class EntityMetaRegistry implements EntityMetaAssembler {
  private EntityMetaFactory $factory;

  @Override
  public void assemble(EntityMetaFactory factory) {
    this.$factory = factory;
    registerPlatform();
    registerDecimalEntry();
  }
  private void registerPlatform() {
      FieldLayout.installGenerated(com.example.exactdecimalservice.platform.Platform.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.exactdecimalservice.platform.Platform.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.exactdecimalservice.platform.Platform.class);
      entityDescriptor.setEntitySupplier(com.example.exactdecimalservice.platform.Platform::new);
      entityDescriptor.with("name", "Platform")

      .with("module", "Organization")

      .with("module_key", "organization")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.exactdecimalservice.platform.Platform.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.exactdecimalservice.platform.Platform.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.exactdecimalservice.platform.Platform.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.exactdecimalservice.platform.Platform.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.exactdecimalservice.platform.Platform.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.exactdecimalservice.platform.Platform.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.exactdecimalservice.platform.Platform.NAME_PROPERTY).with("isPassword", "false")

      .with("max", "100")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("candidates", "Settlement Quote Ledger")

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

      entityDescriptor.findProperty(com.example.exactdecimalservice.platform.Platform.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.exactdecimalservice.platform.Platform.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerDecimalEntry() {
      FieldLayout.installGenerated(com.example.exactdecimalservice.decimalentry.DecimalEntry.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.exactdecimalservice.decimalentry.DecimalEntry.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.exactdecimalservice.decimalentry.DecimalEntry.class);
      entityDescriptor.setEntitySupplier(com.example.exactdecimalservice.decimalentry.DecimalEntry::new);
      entityDescriptor.with("name", "Settlement Quote")

      .with("module", "Ledger")

      .with("module_key", "ledger")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor platform = 
      entityDescriptor.addObjectProperty($factory, com.example.exactdecimalservice.decimalentry.DecimalEntry.PLATFORM_PROPERTY, com.example.exactdecimalservice.platform.Platform.INTERNAL_TYPE, com.example.exactdecimalservice.platform.Platform.DECIMAL_ENTRY_LIST_PROPERTY, com.example.exactdecimalservice.platform.Platform.class)
      ;
      PropertyDescriptor label = 
      entityDescriptor.addSimpleProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.LABEL_PROPERTY, String.class)
      ;
      PropertyDescriptor amount = 
      entityDescriptor.addSimpleProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.AMOUNT_PROPERTY, BigDecimal.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.PLATFORM_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.LABEL_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.LABEL_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.AMOUNT_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.AMOUNT_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.exactdecimalservice.decimalentry.DecimalEntry.VERSION_PROPERTY).with("isPassword", "false")

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