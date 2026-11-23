
package com.example.schoolmanagementservice.schoolcapacitysummary;

import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.platform.PlatformChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class SchoolCapacitySummaryChecker implements Checker<SchoolCapacitySummary>{

    public String type(){
        return SchoolCapacitySummary.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, SchoolCapacitySummary schoolCapacitySummary, ObjectLocation _parentLocation){
        if(needCheck(_context, schoolCapacitySummary)){
            markAsChecked(_context, schoolCapacitySummary);
            doCheck(_context, schoolCapacitySummary, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, SchoolCapacitySummary schoolCapacitySummary, ObjectLocation _parentLocation){
      if((schoolCapacitySummary == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, schoolCapacitySummary);
      if(schoolCapacitySummary.needPersist()){
      }
      if(schoolCapacitySummary.newItem()){
      }else if(schoolCapacitySummary.updateItem()){
        if(!schoolCapacitySummary.isPropertyLoaded("platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "platform"), "Mutation requires a fully loaded entity");
        }
        if(!schoolCapacitySummary.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }
        if(!schoolCapacitySummary.isPropertyLoaded("total_capacity")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "total_capacity"), "Mutation requires a fully loaded entity");
        }
        if(!schoolCapacitySummary.isPropertyLoaded("school_count")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "school_count"), "Mutation requires a fully loaded entity");
        }

      }
      checkPlatform(_context, schoolCapacitySummary.getProperty(SchoolCapacitySummary.PLATFORM_PROPERTY), newLocation(_parentLocation, "platform"));
      checkName(_context, schoolCapacitySummary.getProperty(SchoolCapacitySummary.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      checkTotalCapacity(_context, schoolCapacitySummary.getProperty(SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY), newLocation(_parentLocation, "total_capacity"));
      checkSchoolCount(_context, schoolCapacitySummary.getProperty(SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY), newLocation(_parentLocation, "school_count"));
    }

    public void checkPlatform(UserContext _context, Platform platform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, platform);
    if((platform == null)){
        return;
    }
    new PlatformChecker().checkAndFix(_context, platform, _parentLocation);
    }
    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

    }
    public void checkTotalCapacity(UserContext _context, Integer totalCapacity, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, totalCapacity);
    if((totalCapacity == null)){
        return;
    }
    }
    public void checkSchoolCount(UserContext _context, Integer schoolCount, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, schoolCount);
    if((schoolCount == null)){
        return;
    }
    }
}