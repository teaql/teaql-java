
package com.teaql.merchantorderprivacyservice;

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
    registerMerchant();
    registerCustomerProfile();
    registerCustomerOrder();
  }
  private void registerMerchant() {
      FieldLayout.installGenerated(com.teaql.merchantorderprivacyservice.merchant.Merchant.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.merchantorderprivacyservice.merchant.Merchant.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.merchantorderprivacyservice.merchant.Merchant.class);
      entityDescriptor.setEntitySupplier(com.teaql.merchantorderprivacyservice.merchant.Merchant::new);
      entityDescriptor.with("name", "Merchant")

      .with("module", "Sales")

      .with("module_key", "sales")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.merchantorderprivacyservice.merchant.Merchant.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor name = 
      entityDescriptor.addSimpleProperty(com.teaql.merchantorderprivacyservice.merchant.Merchant.NAME_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.merchantorderprivacyservice.merchant.Merchant.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.merchant.Merchant.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.merchant.Merchant.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.merchant.Merchant.NAME_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.merchant.Merchant.NAME_PROPERTY).with("isPassword", "false")

      .with("max", "100")

      .with("isVersion", "false")

      .with("javaType", "java.lang.String")

      .with("candidates", "Harbour Retail")

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

      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.merchant.Merchant.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.merchant.Merchant.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerCustomerProfile() {
      FieldLayout.installGenerated(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.class);
      entityDescriptor.setEntitySupplier(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile::new);
      entityDescriptor.with("name", "Customer Profile")

      .with("module", "Sales")

      .with("module_key", "sales")

      .with("audit_mask_fields", "email")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.EMAIL_PROPERTY));
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor merchant = 
      entityDescriptor.addObjectProperty($factory, com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.MERCHANT_PROPERTY, com.teaql.merchantorderprivacyservice.merchant.Merchant.INTERNAL_TYPE, com.teaql.merchantorderprivacyservice.merchant.Merchant.CUSTOMER_PROFILE_LIST_PROPERTY, com.teaql.merchantorderprivacyservice.merchant.Merchant.class)
      ;
      PropertyDescriptor email = 
      entityDescriptor.addSimpleProperty(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.EMAIL_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.MERCHANT_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.EMAIL_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.EMAIL_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.VERSION_PROPERTY).with("isPassword", "false")

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
  private void registerCustomerOrder() {
      FieldLayout.installGenerated(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.__TEAQL_FIELD_LAYOUT);
      EntityDescriptor entityDescriptor = new EntityDescriptor();
      entityDescriptor.setType(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.INTERNAL_TYPE);
      entityDescriptor.setTargetType(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.class);
      entityDescriptor.setEntitySupplier(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder::new);
      entityDescriptor.with("name", "Customer Order")

      .with("module", "Sales")

      .with("module_key", "sales")
      ;

      entityDescriptor.setAuditMaskFields(java.util.List.of());
      PropertyDescriptor id = 
      entityDescriptor.addSimpleProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.ID_PROPERTY, Long.class)
      ;
      PropertyDescriptor merchant = 
      entityDescriptor.addObjectProperty($factory, com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.MERCHANT_PROPERTY, com.teaql.merchantorderprivacyservice.merchant.Merchant.INTERNAL_TYPE, com.teaql.merchantorderprivacyservice.merchant.Merchant.CUSTOMER_ORDER_LIST_PROPERTY, com.teaql.merchantorderprivacyservice.merchant.Merchant.class)
      ;
      PropertyDescriptor customerProfile = 
      entityDescriptor.addObjectProperty($factory, com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.CUSTOMER_PROFILE_PROPERTY, com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.INTERNAL_TYPE, com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.CUSTOMER_ORDER_LIST_PROPERTY, com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.class)
      ;
      PropertyDescriptor orderNumber = 
      entityDescriptor.addSimpleProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY, String.class)
      ;
      PropertyDescriptor fulfillmentStatus = 
      entityDescriptor.addSimpleProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.FULFILLMENT_STATUS_PROPERTY, String.class)
      ;
      PropertyDescriptor version = 
      entityDescriptor.addSimpleProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.VERSION_PROPERTY, Long.class)
      ;
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.ID_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.ID_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.MERCHANT_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.CUSTOMER_PROFILE_PROPERTY).with("required", "true");

      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.ORDER_NUMBER_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.FULFILLMENT_STATUS_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.FULFILLMENT_STATUS_PROPERTY).with("isPassword", "false")

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

      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.VERSION_PROPERTY).with("required", "true");
      entityDescriptor.findProperty(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.VERSION_PROPERTY).with("isPassword", "false")

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