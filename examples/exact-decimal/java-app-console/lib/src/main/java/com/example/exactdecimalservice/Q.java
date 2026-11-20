
package com.example.exactdecimalservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.example.exactdecimalservice.platform.PlatformRequest<com.example.exactdecimalservice.platform.Platform> platforms(){
      return new com.example.exactdecimalservice.platform.PlatformRequest(com.example.exactdecimalservice.platform.Platform.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.exactdecimalservice.platform.PlatformRequest<com.example.exactdecimalservice.platform.Platform> platformsWithMinimalFields(){
      return new com.example.exactdecimalservice.platform.PlatformRequest(com.example.exactdecimalservice.platform.Platform.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.example.exactdecimalservice.decimalentry.DecimalEntryRequest<com.example.exactdecimalservice.decimalentry.DecimalEntry> decimalEntries(){
      return new com.example.exactdecimalservice.decimalentry.DecimalEntryRequest(com.example.exactdecimalservice.decimalentry.DecimalEntry.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.exactdecimalservice.decimalentry.DecimalEntryRequest<com.example.exactdecimalservice.decimalentry.DecimalEntry> decimalEntriesWithMinimalFields(){
      return new com.example.exactdecimalservice.decimalentry.DecimalEntryRequest(com.example.exactdecimalservice.decimalentry.DecimalEntry.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}