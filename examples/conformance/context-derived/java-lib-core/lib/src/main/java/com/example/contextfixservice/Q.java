
package com.example.contextfixservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.example.contextfixservice.catalog.CatalogRequest<com.example.contextfixservice.catalog.Catalog> catalogs(){
      return new com.example.contextfixservice.catalog.CatalogRequest(com.example.contextfixservice.catalog.Catalog.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.contextfixservice.catalog.CatalogRequest<com.example.contextfixservice.catalog.Catalog> catalogsWithMinimalFields(){
      return new com.example.contextfixservice.catalog.CatalogRequest(com.example.contextfixservice.catalog.Catalog.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.example.contextfixservice.entry.EntryRequest<com.example.contextfixservice.entry.Entry> entries(){
      return new com.example.contextfixservice.entry.EntryRequest(com.example.contextfixservice.entry.Entry.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.contextfixservice.entry.EntryRequest<com.example.contextfixservice.entry.Entry> entriesWithMinimalFields(){
      return new com.example.contextfixservice.entry.EntryRequest(com.example.contextfixservice.entry.Entry.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}