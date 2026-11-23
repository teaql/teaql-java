
package com.example.freightquotesettlement;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.example.freightquotesettlement.platform.PlatformRequest<com.example.freightquotesettlement.platform.Platform> platforms(){
      return new com.example.freightquotesettlement.platform.PlatformRequest(com.example.freightquotesettlement.platform.Platform.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.freightquotesettlement.platform.PlatformRequest<com.example.freightquotesettlement.platform.Platform> platformsWithMinimalFields(){
      return new com.example.freightquotesettlement.platform.PlatformRequest(com.example.freightquotesettlement.platform.Platform.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.example.freightquotesettlement.freightquote.FreightQuoteRequest<com.example.freightquotesettlement.freightquote.FreightQuote> freightQuotes(){
      return new com.example.freightquotesettlement.freightquote.FreightQuoteRequest(com.example.freightquotesettlement.freightquote.FreightQuote.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.freightquotesettlement.freightquote.FreightQuoteRequest<com.example.freightquotesettlement.freightquote.FreightQuote> freightQuotesWithMinimalFields(){
      return new com.example.freightquotesettlement.freightquote.FreightQuoteRequest(com.example.freightquotesettlement.freightquote.FreightQuote.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}