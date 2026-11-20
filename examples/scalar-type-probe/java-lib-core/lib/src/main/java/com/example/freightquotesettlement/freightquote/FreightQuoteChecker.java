
package com.example.freightquotesettlement.freightquote;

import com.example.freightquotesettlement.platform.Platform;
import com.example.freightquotesettlement.platform.PlatformChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class FreightQuoteChecker implements Checker<FreightQuote>{

    public String type(){
        return FreightQuote.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, FreightQuote freightQuote, ObjectLocation _parentLocation){
        if(needCheck(_context, freightQuote)){
            markAsChecked(_context, freightQuote);
            doCheck(_context, freightQuote, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, FreightQuote freightQuote, ObjectLocation _parentLocation){
      if((freightQuote == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, freightQuote);
      if(freightQuote.needPersist()){
      }
      if(freightQuote.newItem()){
      }else if(freightQuote.updateItem()){
        if(!freightQuote.isPropertyLoaded("platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "platform"), "Mutation requires a fully loaded entity");
        }
        if(!freightQuote.isPropertyLoaded("reference")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "reference"), "Mutation requires a fully loaded entity");
        }
        if(!freightQuote.isPropertyLoaded("amount")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "amount"), "Mutation requires a fully loaded entity");
        }
        if(!freightQuote.isPropertyLoaded("shipment_number")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "shipment_number"), "Mutation requires a fully loaded entity");
        }
        if(!freightQuote.isPropertyLoaded("package_count")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "package_count"), "Mutation requires a fully loaded entity");
        }
        if(!freightQuote.isPropertyLoaded("delivery_date")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "delivery_date"), "Mutation requires a fully loaded entity");
        }
        if(!freightQuote.isPropertyLoaded("pickup_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "pickup_time"), "Mutation requires a fully loaded entity");
        }
        if(!freightQuote.isPropertyLoaded("quoted_at")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "quoted_at"), "Mutation requires a fully loaded entity");
        }
        if(!freightQuote.isPropertyLoaded("instructions")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "instructions"), "Mutation requires a fully loaded entity");
        }

      }
      checkPlatform(_context, freightQuote.getProperty(FreightQuote.PLATFORM_PROPERTY), newLocation(_parentLocation, "platform"));
      checkReference(_context, freightQuote.getProperty(FreightQuote.REFERENCE_PROPERTY), newLocation(_parentLocation, "reference"));
      checkAmount(_context, freightQuote.getProperty(FreightQuote.AMOUNT_PROPERTY), newLocation(_parentLocation, "amount"));
      checkShipmentNumber(_context, freightQuote.getProperty(FreightQuote.SHIPMENT_NUMBER_PROPERTY), newLocation(_parentLocation, "shipment_number"));
      checkPackageCount(_context, freightQuote.getProperty(FreightQuote.PACKAGE_COUNT_PROPERTY), newLocation(_parentLocation, "package_count"));
      checkDeliveryDate(_context, freightQuote.getProperty(FreightQuote.DELIVERY_DATE_PROPERTY), newLocation(_parentLocation, "delivery_date"));
      checkPickupTime(_context, freightQuote.getProperty(FreightQuote.PICKUP_TIME_PROPERTY), newLocation(_parentLocation, "pickup_time"));
      checkQuotedAt(_context, freightQuote.getProperty(FreightQuote.QUOTED_AT_PROPERTY), newLocation(_parentLocation, "quoted_at"));
      checkInstructions(_context, freightQuote.getProperty(FreightQuote.INSTRUCTIONS_PROPERTY), newLocation(_parentLocation, "instructions"));
    }

    public void checkPlatform(UserContext _context, Platform platform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, platform);
    if((platform == null)){
        return;
    }
    new PlatformChecker().checkAndFix(_context, platform, _parentLocation);
    }
    public void checkReference(UserContext _context, String reference, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, reference);
    if((reference == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, reference);

    }
    public void checkAmount(UserContext _context, BigDecimal amount, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, amount);
    if((amount == null)){
        return;
    }
    }
    public void checkShipmentNumber(UserContext _context, Long shipmentNumber, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, shipmentNumber);
    if((shipmentNumber == null)){
        return;
    }
    }
    public void checkPackageCount(UserContext _context, Integer packageCount, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, packageCount);
    if((packageCount == null)){
        return;
    }
    }
    public void checkDeliveryDate(UserContext _context, LocalDate deliveryDate, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, deliveryDate);
    if((deliveryDate == null)){
        return;
    }
    }
    public void checkPickupTime(UserContext _context, LocalTime pickupTime, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, pickupTime);
    if((pickupTime == null)){
        return;
    }
    }
    public void checkQuotedAt(UserContext _context, LocalDateTime quotedAt, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, quotedAt);
    if((quotedAt == null)){
        return;
    }
    }
    public void checkInstructions(UserContext _context, String instructions, ObjectLocation _parentLocation){
    if((instructions == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, instructions);

    }
}