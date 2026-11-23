
package com.example.checkernaturalmessageservice.catalog;

import com.example.checkernaturalmessageservice.entry.Entry;
import com.example.checkernaturalmessageservice.entry.EntryChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class CatalogChecker implements Checker<Catalog>{

    public String type(){
        return Catalog.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Catalog catalog, ObjectLocation _parentLocation){
        if(needCheck(_context, catalog)){
            markAsChecked(_context, catalog);
            doCheck(_context, catalog, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Catalog catalog, ObjectLocation _parentLocation){
      if((catalog == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, catalog);
      if(catalog.needPersist()){
      }
      if(catalog.newItem()){
      }else if(catalog.updateItem()){
        if(!catalog.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, catalog.getProperty(Catalog.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      for(int i = 0; catalog.getEntryList() != null && i < catalog.getEntryList().size(); i++){
         Entry entry = catalog.getEntryList().get(i);
         new EntryChecker().checkAndFix(_context, entry, newLocation(_parentLocation, "entry_list", i));
      }
    }

    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

    }
}