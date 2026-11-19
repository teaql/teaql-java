
package com.example.inheritancematchingservice;

import io.teaql.core.FieldLayout;
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
    registerSchoolType();
    registerSchool();
    registerAcademy();
    registerTrainingAcademy();
    registerCollege();
  }
  private void registerPlatform() {
      FieldLayout.installGenerated(com.example.inheritancematchingservice.platform.Platform.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.inheritancematchingservice.platform.Platform.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.inheritancematchingservice.platform.Platform.class);
      entityDescriptor.setEntitySupplier(com.example.inheritancematchingservice.platform.Platform::new);
      entityDescriptor.with("name", "Platform")

      .with("module", "Organization")

      .with("module_key", "organization")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.platform.Platform.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.platform.Platform.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor baseUrl = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.platform.Platform.BASE_URL_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.platform.Platform.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.inheritancematchingservice.platform.Platform.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.platform.Platform.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.inheritancematchingservice.platform.Platform.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.platform.Platform.NAME_PROPERTY).with("isPassword", "false")

      .with("max", "100")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("candidates", "Inheritance Campus")

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

      entityDescriptor.findProperty(com.example.inheritancematchingservice.platform.Platform.BASE_URL_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.platform.Platform.BASE_URL_PROPERTY).with("isPassword", "false")

      .with("max", "100")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("candidates", "https://inheritance.example.com")

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

      entityDescriptor.findProperty(com.example.inheritancematchingservice.platform.Platform.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.platform.Platform.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerSchoolType() {
      FieldLayout.installGenerated(com.example.inheritancematchingservice.schooltype.SchoolType.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.inheritancematchingservice.schooltype.SchoolType.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.inheritancematchingservice.schooltype.SchoolType.class);
      entityDescriptor.setEntitySupplier(com.example.inheritancematchingservice.schooltype.SchoolType::new);
      entityDescriptor.with("name", "School Type")

      .with("module", "Academics")

      .with("module_key", "academics")

      .with("constant", "true")

      .with("identifier", "code")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor platform = 
      entityDescriptor.addObjectProperty($factory, com.example.inheritancematchingservice.schooltype.SchoolType.PLATFORM_PROPERTY, com.example.inheritancematchingservice.platform.Platform.INTERNAL_TYPE, com.example.inheritancematchingservice.platform.Platform.SCHOOL_TYPE_LIST_PROPERTY, com.example.inheritancematchingservice.platform.Platform.class)
      ;
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.schooltype.SchoolType.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.schooltype.SchoolType.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor code = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.schooltype.SchoolType.CODE_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.schooltype.SchoolType.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.inheritancematchingservice.schooltype.SchoolType.PLATFORM_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.schooltype.SchoolType.PLATFORM_PROPERTY).with("candidates", "platform()")
      ;

      entityDescriptor.findProperty(com.example.inheritancematchingservice.schooltype.SchoolType.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.schooltype.SchoolType.ID_PROPERTY).with("isPassword", "false")

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

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.example.inheritancematchingservice.schooltype.SchoolType.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.schooltype.SchoolType.NAME_PROPERTY).with("isPassword", "false")

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

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.example.inheritancematchingservice.schooltype.SchoolType.CODE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.schooltype.SchoolType.CODE_PROPERTY).with("identifier", "true")

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

      .with("isText", "false")
      ;

      entityDescriptor.findProperty(com.example.inheritancematchingservice.schooltype.SchoolType.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.schooltype.SchoolType.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerSchool() {
      FieldLayout.installGenerated(com.example.inheritancematchingservice.school.School.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.inheritancematchingservice.school.School.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.inheritancematchingservice.school.School.class);
      entityDescriptor.setEntitySupplier(com.example.inheritancematchingservice.school.School::new);
      entityDescriptor.with("name", "School")

      .with("module", "Academics")

      .with("module_key", "academics")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.school.School.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor platform = 
      entityDescriptor.addObjectProperty($factory, com.example.inheritancematchingservice.school.School.PLATFORM_PROPERTY, com.example.inheritancematchingservice.platform.Platform.INTERNAL_TYPE, com.example.inheritancematchingservice.platform.Platform.SCHOOL_LIST_PROPERTY, com.example.inheritancematchingservice.platform.Platform.class)
      ;
      PropertyDescriptor schoolType = 
      entityDescriptor.addObjectProperty($factory, com.example.inheritancematchingservice.school.School.SCHOOL_TYPE_PROPERTY, com.example.inheritancematchingservice.schooltype.SchoolType.INTERNAL_TYPE, com.example.inheritancematchingservice.schooltype.SchoolType.SCHOOL_LIST_PROPERTY, com.example.inheritancematchingservice.schooltype.SchoolType.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.school.School.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor studentCapacity = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.school.School.STUDENT_CAPACITY_PROPERTY, Integer.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.school.School.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.example.inheritancematchingservice.school.School.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.school.School.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.inheritancematchingservice.school.School.PLATFORM_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.inheritancematchingservice.school.School.SCHOOL_TYPE_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.example.inheritancematchingservice.school.School.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.school.School.NAME_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.inheritancematchingservice.school.School.STUDENT_CAPACITY_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.school.School.STUDENT_CAPACITY_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.inheritancematchingservice.school.School.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.school.School.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerAcademy() {
      FieldLayout.installGenerated(com.example.inheritancematchingservice.academy.Academy.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.inheritancematchingservice.academy.Academy.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.inheritancematchingservice.academy.Academy.class);
      entityDescriptor.setEntitySupplier(com.example.inheritancematchingservice.academy.Academy::new);
      entityDescriptor.with("name", "Academy")

      .with("module", "Academics")

      .with("module_key", "academics")

      .with("parent", "school")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.academy.Academy.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor campusCode = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.academy.Academy.CAMPUS_CODE_PROPERTY, String.class)
      ;
      entityDescriptor.setParent($factory.resolveEntityDescriptor(com.example.inheritancematchingservice.school.School.INTERNAL_TYPE));
      entityDescriptor.findProperty(com.example.inheritancematchingservice.academy.Academy.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.academy.Academy.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.inheritancematchingservice.academy.Academy.CAMPUS_CODE_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.academy.Academy.CAMPUS_CODE_PROPERTY).with("isPassword", "false")

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

      $factory.register(entityDescriptor);
  }
  private void registerTrainingAcademy() {
      FieldLayout.installGenerated(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.class);
      entityDescriptor.setEntitySupplier(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy::new);
      entityDescriptor.with("name", "Training Academy")

      .with("module", "Academics")

      .with("module_key", "academics")

      .with("parent", "academy")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor specialty = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.SPECIALTY_PROPERTY, String.class)
      ;
      entityDescriptor.setParent($factory.resolveEntityDescriptor(com.example.inheritancematchingservice.academy.Academy.INTERNAL_TYPE));
      entityDescriptor.findProperty(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.SPECIALTY_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.SPECIALTY_PROPERTY).with("isPassword", "false")

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

      $factory.register(entityDescriptor);
  }
  private void registerCollege() {
      FieldLayout.installGenerated(com.example.inheritancematchingservice.college.College.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.example.inheritancematchingservice.college.College.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.example.inheritancematchingservice.college.College.class);
      entityDescriptor.setEntitySupplier(com.example.inheritancematchingservice.college.College::new);
      entityDescriptor.with("name", "College")

      .with("module", "Academics")

      .with("module_key", "academics")

      .with("parent", "school")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.college.College.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor degreeLevel = 
      entityDescriptor.addSimpleProperty(com.example.inheritancematchingservice.college.College.DEGREE_LEVEL_PROPERTY, String.class)
      ;
      entityDescriptor.setParent($factory.resolveEntityDescriptor(com.example.inheritancematchingservice.school.School.INTERNAL_TYPE));
      entityDescriptor.findProperty(com.example.inheritancematchingservice.college.College.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.college.College.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.example.inheritancematchingservice.college.College.DEGREE_LEVEL_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.example.inheritancematchingservice.college.College.DEGREE_LEVEL_PROPERTY).with("isPassword", "false")

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

      $factory.register(entityDescriptor);
  }
}