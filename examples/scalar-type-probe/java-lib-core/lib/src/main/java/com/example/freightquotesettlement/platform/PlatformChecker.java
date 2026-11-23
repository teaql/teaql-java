
package com.example.freightquotesettlement.platform;

import com.example.freightquotesettlement.freightquote.FreightQuote;
import com.example.freightquotesettlement.freightquote.FreightQuoteChecker;
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
      for(int i = 0; platform.getFreightQuoteList() != null && i < platform.getFreightQuoteList().size(); i++){
         FreightQuote freightQuote = platform.getFreightQuoteList().get(i);
         new FreightQuoteChecker().checkAndFix(_context, freightQuote, newLocation(_parentLocation, "freight_quote_list", i));
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