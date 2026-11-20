
package com.example.exactdecimalservice.platform;

import com.example.exactdecimalservice.decimalentry.DecimalEntry;
import com.example.exactdecimalservice.decimalentry.DecimalEntryChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class PlatformChecker implements Checker<Platform>{

    public String type(){
        return Platform.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Platform platform, ObjectLocation _parentLocation){
        if(needCheck(_context, platform)){
            markAsChecked(_context, platform);
            doCheck(_context, platform, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Platform platform, ObjectLocation _parentLocation){
      if((platform == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, platform);
      if(platform.needPersist()){
      }
      if(platform.newItem()){
      }else if(platform.updateItem()){
        if(!platform.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, platform.getProperty(Platform.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      for(int i = 0; platform.getDecimalEntryList() != null && i < platform.getDecimalEntryList().size(); i++){
         DecimalEntry decimalEntry = platform.getDecimalEntryList().get(i);
         new DecimalEntryChecker().checkAndFix(_context, decimalEntry, newLocation(_parentLocation, "decimal_entry_list", i));
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