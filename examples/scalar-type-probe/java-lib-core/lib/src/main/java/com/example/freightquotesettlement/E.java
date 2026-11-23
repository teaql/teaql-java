
package com.example.freightquotesettlement;

import com.example.freightquotesettlement.freightquote.FreightQuote;
import com.example.freightquotesettlement.freightquote.FreightQuoteExpression;
import com.example.freightquotesettlement.platform.Platform;
import com.example.freightquotesettlement.platform.PlatformExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static PlatformExpression<Platform, Platform, Platform> platform(Platform platform){
      return new PlatformExpression(new ValueExpression(platform, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.platform(<root>)"));
  }
  public static FreightQuoteExpression<FreightQuote, FreightQuote, FreightQuote> freightQuote(FreightQuote freightQuote){
      return new FreightQuoteExpression(new ValueExpression(freightQuote, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.freightQuote(<root>)"));
  }
}