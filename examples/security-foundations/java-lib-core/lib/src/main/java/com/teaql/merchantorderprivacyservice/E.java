
package com.teaql.merchantorderprivacyservice;

import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder;
import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrderExpression;
import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile;
import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfileExpression;
import com.teaql.merchantorderprivacyservice.merchant.Merchant;
import com.teaql.merchantorderprivacyservice.merchant.MerchantExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static MerchantExpression<Merchant, Merchant, Merchant> merchant(Merchant merchant){
      return new MerchantExpression(new ValueExpression(merchant, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.merchant(<root>)"));
  }
  public static CustomerProfileExpression<CustomerProfile, CustomerProfile, CustomerProfile> customerProfile(CustomerProfile customerProfile){
      return new CustomerProfileExpression(new ValueExpression(customerProfile, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.customerProfile(<root>)"));
  }
  public static CustomerOrderExpression<CustomerOrder, CustomerOrder, CustomerOrder> customerOrder(CustomerOrder customerOrder){
      return new CustomerOrderExpression(new ValueExpression(customerOrder, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.customerOrder(<root>)"));
  }
}