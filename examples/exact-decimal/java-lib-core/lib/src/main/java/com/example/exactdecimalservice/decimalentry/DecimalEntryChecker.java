
package com.example.exactdecimalservice.decimalentry;

import com.example.exactdecimalservice.platform.Platform;
import com.example.exactdecimalservice.platform.PlatformChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.math.BigDecimal;

public class DecimalEntryChecker implements Checker<DecimalEntry>{

    public String type(){
        return DecimalEntry.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, DecimalEntry decimalEntry, ObjectLocation _parentLocation){
        if(needCheck(_context, decimalEntry)){
            markAsChecked(_context, decimalEntry);
            doCheck(_context, decimalEntry, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, DecimalEntry decimalEntry, ObjectLocation _parentLocation){
      if((decimalEntry == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, decimalEntry);
      if(decimalEntry.needPersist()){
      }
      if(decimalEntry.newItem()){
      }else if(decimalEntry.updateItem()){
        if(!decimalEntry.isPropertyLoaded("platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "platform"), "Mutation requires a fully loaded entity");
        }
        if(!decimalEntry.isPropertyLoaded("label")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "label"), "Mutation requires a fully loaded entity");
        }
        if(!decimalEntry.isPropertyLoaded("amount")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "amount"), "Mutation requires a fully loaded entity");
        }

      }
      checkPlatform(_context, decimalEntry.getProperty(DecimalEntry.PLATFORM_PROPERTY), newLocation(_parentLocation, "platform"));
      checkLabel(_context, decimalEntry.getProperty(DecimalEntry.LABEL_PROPERTY), newLocation(_parentLocation, "label"));
      checkAmount(_context, decimalEntry.getProperty(DecimalEntry.AMOUNT_PROPERTY), newLocation(_parentLocation, "amount"));
    }

    public void checkPlatform(UserContext _context, Platform platform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, platform);
    if((platform == null)){
        return;
    }
    new PlatformChecker().checkAndFix(_context, platform, _parentLocation);
    }
    public void checkLabel(UserContext _context, String label, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, label);
    if((label == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, label);

    }
    public void checkAmount(UserContext _context, BigDecimal amount, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, amount);
    if((amount == null)){
        return;
    }
    }
}