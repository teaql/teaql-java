
package com.example.scalartypeconformanceservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.example.scalartypeconformanceservice.typeplatform.TypePlatformRequest<com.example.scalartypeconformanceservice.typeplatform.TypePlatform> typePlatforms(){
      return new com.example.scalartypeconformanceservice.typeplatform.TypePlatformRequest(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.scalartypeconformanceservice.typeplatform.TypePlatformRequest<com.example.scalartypeconformanceservice.typeplatform.TypePlatform> typePlatformsWithMinimalFields(){
      return new com.example.scalartypeconformanceservice.typeplatform.TypePlatformRequest(com.example.scalartypeconformanceservice.typeplatform.TypePlatform.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbeRequest<com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe> scalarTypeProbes(){
      return new com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbeRequest(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbeRequest<com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe> scalarTypeProbesWithMinimalFields(){
      return new com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbeRequest(com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}