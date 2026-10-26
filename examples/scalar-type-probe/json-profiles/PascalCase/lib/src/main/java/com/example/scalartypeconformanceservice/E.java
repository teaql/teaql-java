
package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbeExpression;
import com.example.scalartypeconformanceservice.typeplatform.TypePlatform;
import com.example.scalartypeconformanceservice.typeplatform.TypePlatformExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static TypePlatformExpression<TypePlatform, TypePlatform, TypePlatform> typePlatform(TypePlatform typePlatform){
      return new TypePlatformExpression(new ValueExpression(typePlatform, JsonFieldNamingProfile.fromModelValue("PascalCase"), "E.typePlatform(<root>)"));
  }
  public static ScalarTypeProbeExpression<ScalarTypeProbe, ScalarTypeProbe, ScalarTypeProbe> scalarTypeProbe(ScalarTypeProbe scalarTypeProbe){
      return new ScalarTypeProbeExpression(new ValueExpression(scalarTypeProbe, JsonFieldNamingProfile.fromModelValue("PascalCase"), "E.scalarTypeProbe(<root>)"));
  }
}