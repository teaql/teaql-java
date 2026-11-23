
package com.example.checkernaturalmessageservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.example.checkernaturalmessageservice.catalog.CatalogRequest<com.example.checkernaturalmessageservice.catalog.Catalog> catalogs(){
      return new com.example.checkernaturalmessageservice.catalog.CatalogRequest(com.example.checkernaturalmessageservice.catalog.Catalog.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.checkernaturalmessageservice.catalog.CatalogRequest<com.example.checkernaturalmessageservice.catalog.Catalog> catalogsWithMinimalFields(){
      return new com.example.checkernaturalmessageservice.catalog.CatalogRequest(com.example.checkernaturalmessageservice.catalog.Catalog.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.example.checkernaturalmessageservice.entry.EntryRequest<com.example.checkernaturalmessageservice.entry.Entry> entries(){
      return new com.example.checkernaturalmessageservice.entry.EntryRequest(com.example.checkernaturalmessageservice.entry.Entry.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.checkernaturalmessageservice.entry.EntryRequest<com.example.checkernaturalmessageservice.entry.Entry> entriesWithMinimalFields(){
      return new com.example.checkernaturalmessageservice.entry.EntryRequest(com.example.checkernaturalmessageservice.entry.Entry.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}