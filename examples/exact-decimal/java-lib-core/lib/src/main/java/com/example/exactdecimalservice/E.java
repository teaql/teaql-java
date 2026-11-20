
package com.example.exactdecimalservice;

import com.example.exactdecimalservice.decimalentry.DecimalEntry;
import com.example.exactdecimalservice.decimalentry.DecimalEntryExpression;
import com.example.exactdecimalservice.platform.Platform;
import com.example.exactdecimalservice.platform.PlatformExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static PlatformExpression<Platform, Platform, Platform> platform(Platform platform){
      return new PlatformExpression(new ValueExpression(platform, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.platform(<root>)"));
  }
  public static DecimalEntryExpression<DecimalEntry, DecimalEntry, DecimalEntry> decimalEntry(DecimalEntry decimalEntry){
      return new DecimalEntryExpression(new ValueExpression(decimalEntry, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.decimalEntry(<root>)"));
  }
}