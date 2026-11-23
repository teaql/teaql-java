
package com.example.inheritancematchingservice.school;

import com.example.inheritancematchingservice.platform.Platform;
import com.example.inheritancematchingservice.platform.PlatformChecker;
import com.example.inheritancematchingservice.schooltype.SchoolType;
import com.example.inheritancematchingservice.schooltype.SchoolTypeChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class SchoolChecker implements Checker<School>{

    public String type(){
        return School.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, School school, ObjectLocation _parentLocation){
        if(needCheck(_context, school)){
            markAsChecked(_context, school);
            doCheck(_context, school, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, School school, ObjectLocation _parentLocation){
      if((school == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, school);
      if(school.needPersist()){
      }
      if(school.newItem()){
      }else if(school.updateItem()){
        if(!school.isPropertyLoaded("platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "platform"), "Mutation requires a fully loaded entity");
        }
        if(!school.isPropertyLoaded("school_type")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "school_type"), "Mutation requires a fully loaded entity");
        }
        if(!school.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }
        if(!school.isPropertyLoaded("student_capacity")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "student_capacity"), "Mutation requires a fully loaded entity");
        }

      }
      checkPlatform(_context, school.getProperty(School.PLATFORM_PROPERTY), newLocation(_parentLocation, "platform"));
      checkSchoolType(_context, school.getProperty(School.SCHOOL_TYPE_PROPERTY), newLocation(_parentLocation, "school_type"));
      checkName(_context, school.getProperty(School.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      checkStudentCapacity(_context, school.getProperty(School.STUDENT_CAPACITY_PROPERTY), newLocation(_parentLocation, "student_capacity"));
    }

    public void checkPlatform(UserContext _context, Platform platform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, platform);
    if((platform == null)){
        return;
    }
    new PlatformChecker().checkAndFix(_context, platform, _parentLocation);
    }
    public void checkSchoolType(UserContext _context, SchoolType schoolType, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, schoolType);
    if((schoolType == null)){
        return;
    }
    new SchoolTypeChecker().checkAndFix(_context, schoolType, _parentLocation);
    }
    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

    }
    public void checkStudentCapacity(UserContext _context, Integer studentCapacity, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, studentCapacity);
    if((studentCapacity == null)){
        return;
    }
    }
}