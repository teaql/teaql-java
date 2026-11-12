
package com.example.checkernaturalmessageservice;

import com.example.checkernaturalmessageservice.catalog.Catalog;

public interface Constants  {
  public static final long CATALOG_ID = 1l;
  public static final Catalog CATALOG = Catalog.refer(CATALOG_ID);
}