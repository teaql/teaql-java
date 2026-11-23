
package com.example.contextfixservice;

import com.example.contextfixservice.catalog.Catalog;

public interface Constants  {
  public static final long CATALOG_ID = 1l;
  public static final Catalog CATALOG = Catalog.refer(CATALOG_ID);
}