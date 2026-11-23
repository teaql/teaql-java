
package com.example.inheritancematchingservice.college;

import com.example.inheritancematchingservice.school.SchoolChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class CollegeChecker implements Checker<College>{

    public String type(){
        return College.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, College college, ObjectLocation _parentLocation){
        if(needCheck(_context, college)){
            markAsChecked(_context, college);
            new SchoolChecker().doCheck(_context, college, _parentLocation);

            doCheck(_context, college, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, College college, ObjectLocation _parentLocation){
      if((college == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, college);
      if(college.needPersist()){
      }
      if(college.newItem()){
      }else if(college.updateItem()){
        if(!college.isPropertyLoaded("degree_level")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "degree_level"), "Mutation requires a fully loaded entity");
        }

      }
      checkDegreeLevel(_context, college.getProperty(College.DEGREE_LEVEL_PROPERTY), newLocation(_parentLocation, "degree_level"));
    }

    public void checkDegreeLevel(UserContext _context, String degreeLevel, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, degreeLevel);
    if((degreeLevel == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, degreeLevel);

    }
}