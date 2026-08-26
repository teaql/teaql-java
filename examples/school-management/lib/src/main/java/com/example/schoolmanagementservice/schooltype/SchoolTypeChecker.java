
package com.example.schoolmanagementservice.schooltype;

import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.platform.PlatformChecker;
import com.example.schoolmanagementservice.school.School;
import com.example.schoolmanagementservice.school.SchoolChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.math.BigDecimal;

public class SchoolTypeChecker implements Checker<SchoolType>{

    public String type(){
        return SchoolType.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, SchoolType schoolType, ObjectLocation _parentLocation){
        if(needCheck(_context, schoolType)){
            markAsChecked(_context, schoolType);
            doCheck(_context, schoolType, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, SchoolType schoolType, ObjectLocation _parentLocation){
      if((schoolType == null)){
         return;
      }
      if(schoolType.newItem()){
      }else if(schoolType.updateItem()){
      }
      checkPlatform(_context, schoolType.getProperty(SchoolType.PLATFORM_PROPERTY), newLocation(_parentLocation, SchoolType.PLATFORM_PROPERTY));
      checkName(_context, schoolType.getProperty(SchoolType.NAME_PROPERTY), newLocation(_parentLocation, SchoolType.NAME_PROPERTY));
      checkCode(_context, schoolType.getProperty(SchoolType.CODE_PROPERTY), newLocation(_parentLocation, SchoolType.CODE_PROPERTY));
      checkDisplayOrder(_context, schoolType.getProperty(SchoolType.DISPLAY_ORDER_PROPERTY), newLocation(_parentLocation, SchoolType.DISPLAY_ORDER_PROPERTY));
      for(int i = 0; schoolType.getSchoolList() != null && i < schoolType.getSchoolList().size(); i++){
         School school = schoolType.getSchoolList().get(i);
         new SchoolChecker().checkAndFix(_context, school, newLocation(_parentLocation, SchoolType.SCHOOL_LIST_PROPERTY, i));
      }
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
    public void checkCode(UserContext _context, String code, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, code);
    if((code == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, code);

    }
    public void checkDisplayOrder(UserContext _context, BigDecimal displayOrder, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, displayOrder);
    if((displayOrder == null)){
        return;
    }
    }
}