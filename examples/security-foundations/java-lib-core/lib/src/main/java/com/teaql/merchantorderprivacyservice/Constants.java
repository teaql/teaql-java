
package com.teaql.merchantorderprivacyservice;

import com.teaql.merchantorderprivacyservice.merchant.Merchant;

public interface Constants  {
  public static final long MERCHANT_ID = 1l;
  public static final Merchant MERCHANT = Merchant.refer(MERCHANT_ID);
}