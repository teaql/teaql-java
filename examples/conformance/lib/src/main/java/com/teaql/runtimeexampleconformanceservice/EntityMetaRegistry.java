
package com.teaql.runtimeexampleconformanceservice;

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
    registerWorkItem();
  }
  private void registerPlatform() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.runtimeexampleconformanceservice.platform.Platform.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.runtimeexampleconformanceservice.platform.Platform.class);
      entityDescriptor.setEntitySupplier(com.teaql.runtimeexampleconformanceservice.platform.Platform::new);
      entityDescriptor.with("name", "Platform")
      .with("module", "Example")
      .with("module_key", "example");

      PropertyDescriptor id =
      entityDescriptor.addSimpleProperty(com.teaql.runtimeexampleconformanceservice.platform.Platform.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name =
      entityDescriptor.addSimpleProperty(com.teaql.runtimeexampleconformanceservice.platform.Platform.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version =
      entityDescriptor.addSimpleProperty(com.teaql.runtimeexampleconformanceservice.platform.Platform.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.runtimeexampleconformanceservice.platform.Platform.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.runtimeexampleconformanceservice.platform.Platform.NAME_PROPERTY).with("isPassword", "false")
      .with("max", "100")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("candidates", "Runtime Example")
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

      entityDescriptor.findProperty(com.teaql.runtimeexampleconformanceservice.platform.Platform.VERSION_PROPERTY).with("isPassword", "false")
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
  private void registerWorkItem() {
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.class);
      entityDescriptor.setEntitySupplier(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem::new);
      entityDescriptor.with("name", "Work Item")
      .with("module", "Example")
      .with("module_key", "example");

      PropertyDescriptor id =
      entityDescriptor.addSimpleProperty(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor title =
      entityDescriptor.addSimpleProperty(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.TITLE_PROPERTY, String.class)
      ;
      PropertyDescriptor description =
      entityDescriptor.addSimpleProperty(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.DESCRIPTION_PROPERTY, String.class)
      ;
      PropertyDescriptor platform =
      entityDescriptor.addObjectProperty($factory, com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.PLATFORM_PROPERTY, com.teaql.runtimeexampleconformanceservice.platform.Platform.INTERNAL_TYPE, com.teaql.runtimeexampleconformanceservice.platform.Platform.WORK_ITEM_LIST_PROPERTY, com.teaql.runtimeexampleconformanceservice.platform.Platform.class)
      ;
      PropertyDescriptor version =
      entityDescriptor.addSimpleProperty(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.ID_PROPERTY).with("isPassword", "false")
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

      entityDescriptor.findProperty(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.TITLE_PROPERTY).with("isPassword", "false")
      .with("max", "80")
      .with("isVersion", "false")
      .with("javaType", "java.lang.String")
      .with("sqlType", "VARCHAR(<max>)")
      .with("min", "1")
      .with("isId", "false")
      .with("isBool", "false")
      .with("isBaseEntityField", "false")
      .with("isNumber", "false")
      .with("isString", "true")
      .with("isDate", "false")
      .with("graphqlType", "String")
      .with("isTime", "false")
      .with("isText", "false");

      entityDescriptor.findProperty(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.DESCRIPTION_PROPERTY).with("isPassword", "false")
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


      entityDescriptor.findProperty(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.VERSION_PROPERTY).with("isPassword", "false")
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