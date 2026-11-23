
package com.example.contextfixservice;

import com.example.contextfixservice.catalog.Catalog;
import com.example.contextfixservice.catalog.CatalogExpression;
import com.example.contextfixservice.entry.Entry;
import com.example.contextfixservice.entry.EntryExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static CatalogExpression<Catalog, Catalog, Catalog> catalog(Catalog catalog){
      return new CatalogExpression(new ValueExpression(catalog, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.catalog(<root>)"));
  }
  public static EntryExpression<Entry, Entry, Entry> entry(Entry entry){
      return new EntryExpression(new ValueExpression(entry, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.entry(<root>)"));
  }
}