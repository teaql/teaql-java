
package com.teaql.merchantorderprivacyservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.teaql.merchantorderprivacyservice.merchant.MerchantRequest<com.teaql.merchantorderprivacyservice.merchant.Merchant> merchants(){
      return new com.teaql.merchantorderprivacyservice.merchant.MerchantRequest(com.teaql.merchantorderprivacyservice.merchant.Merchant.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.merchantorderprivacyservice.merchant.MerchantRequest<com.teaql.merchantorderprivacyservice.merchant.Merchant> merchantsWithMinimalFields(){
      return new com.teaql.merchantorderprivacyservice.merchant.MerchantRequest(com.teaql.merchantorderprivacyservice.merchant.Merchant.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfileRequest<com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile> customerProfiles(){
      return new com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfileRequest(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfileRequest<com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile> customerProfilesWithMinimalFields(){
      return new com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfileRequest(com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.teaql.merchantorderprivacyservice.customerorder.CustomerOrderRequest<com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder> customerOrders(){
      return new com.teaql.merchantorderprivacyservice.customerorder.CustomerOrderRequest(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.merchantorderprivacyservice.customerorder.CustomerOrderRequest<com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder> customerOrdersWithMinimalFields(){
      return new com.teaql.merchantorderprivacyservice.customerorder.CustomerOrderRequest(com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}