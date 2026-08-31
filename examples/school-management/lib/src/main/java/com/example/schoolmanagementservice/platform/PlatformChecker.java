
package com.example.schoolmanagementservice.platform;

import com.example.schoolmanagementservice.school.School;
import com.example.schoolmanagementservice.school.SchoolChecker;
import com.example.schoolmanagementservice.schooltype.SchoolType;
import com.example.schoolmanagementservice.schooltype.SchoolTypeChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.time.LocalDateTime;

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
      if(platform.newItem()){
        if(platform.getCreateTime() == null){
           platform.updateCreateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "create_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        }if(platform.getUpdateTime() == null){
           platform.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        }
      }else if(platform.updateItem()){
        platform.updateUpdateTime(_context.evaluate("now"));
        _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        if(!platform.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }if(!platform.isPropertyLoaded("baseUrl")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "base_url"), "Mutation requires a fully loaded entity");
        }if(!platform.isPropertyLoaded("createTime")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "create_time"), "Mutation requires a fully loaded entity");
        }if(!platform.isPropertyLoaded("updateTime")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "update_time"), "Mutation requires a fully loaded entity");
        }
      }
      checkName(_context, platform.getProperty(Platform.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      checkBaseUrl(_context, platform.getProperty(Platform.BASE_URL_PROPERTY), newLocation(_parentLocation, "base_url"));
      checkCreateTime(_context, platform.getProperty(Platform.CREATE_TIME_PROPERTY), newLocation(_parentLocation, "create_time"));
      checkUpdateTime(_context, platform.getProperty(Platform.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, "update_time"));
      for(int i = 0; platform.getSchoolTypeList() != null && i < platform.getSchoolTypeList().size(); i++){
         SchoolType schoolType = platform.getSchoolTypeList().get(i);
         new SchoolTypeChecker().checkAndFix(_context, schoolType, newLocation(_parentLocation, "school_type_list", i));
      }
      for(int i = 0; platform.getSchoolList() != null && i < platform.getSchoolList().size(); i++){
         School school = platform.getSchoolList().get(i);
         new SchoolChecker().checkAndFix(_context, school, newLocation(_parentLocation, "school_list", i));
      }
    }

    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

    }
    public void checkBaseUrl(UserContext _context, String baseUrl, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, baseUrl);
    if((baseUrl == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, baseUrl);

    }
    public void checkCreateTime(UserContext _context, LocalDateTime createTime, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, createTime);
    if((createTime == null)){
        return;
    }
    }
    public void checkUpdateTime(UserContext _context, LocalDateTime updateTime, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, updateTime);
    if((updateTime == null)){
        return;
    }
    }
}