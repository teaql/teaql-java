
package com.teaql.purchaseordernumbering;

import com.teaql.purchaseordernumbering.platform.Platform;
import com.teaql.purchaseordernumbering.platform.PlatformExpression;
import com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder;
import com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrderExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static PlatformExpression<Platform, Platform, Platform> platform(Platform platform){
      return new PlatformExpression(new ValueExpression(platform, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.platform(<root>)"));
  }
  public static PurchaseOrderExpression<PurchaseOrder, PurchaseOrder, PurchaseOrder> purchaseOrder(PurchaseOrder purchaseOrder){
      return new PurchaseOrderExpression(new ValueExpression(purchaseOrder, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.purchaseOrder(<root>)"));
  }
}