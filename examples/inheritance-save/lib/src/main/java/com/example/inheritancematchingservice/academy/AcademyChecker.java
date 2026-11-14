
package com.example.inheritancematchingservice.academy;

import com.example.inheritancematchingservice.school.SchoolChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class AcademyChecker implements Checker<Academy>{

    public String type(){
        return Academy.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Academy academy, ObjectLocation _parentLocation){
        if(needCheck(_context, academy)){
            markAsChecked(_context, academy);
            new SchoolChecker().doCheck(_context, academy, _parentLocation);

            doCheck(_context, academy, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Academy academy, ObjectLocation _parentLocation){
      if((academy == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, academy);
      if(academy.needPersist()){
      }
      if(academy.newItem()){
      }else if(academy.updateItem()){
        if(!academy.isPropertyLoaded("campus_code")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "campus_code"), "Mutation requires a fully loaded entity");
        }

      }
      checkCampusCode(_context, academy.getProperty(Academy.CAMPUS_CODE_PROPERTY), newLocation(_parentLocation, "campus_code"));
    }

    public void checkCampusCode(UserContext _context, String campusCode, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, campusCode);
    if((campusCode == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, campusCode);

    }
}