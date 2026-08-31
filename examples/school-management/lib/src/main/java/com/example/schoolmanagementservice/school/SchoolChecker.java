
package com.example.schoolmanagementservice.school;

import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.platform.PlatformChecker;
import com.example.schoolmanagementservice.schooltype.SchoolType;
import com.example.schoolmanagementservice.schooltype.SchoolTypeChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
      if(school.newItem()){
        if(school.getCreateTime() == null){
           school.updateCreateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "create_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        }if(school.getUpdateTime() == null){
           school.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        }
      }else if(school.updateItem()){
        school.updateUpdateTime(_context.evaluate("now"));
        _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        if(!school.isPropertyLoaded("platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "platform"), "Mutation requires a fully loaded entity");
        }if(!school.isPropertyLoaded("schoolType")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "school_type"), "Mutation requires a fully loaded entity");
        }if(!school.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }if(!school.isPropertyLoaded("address")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "address"), "Mutation requires a fully loaded entity");
        }if(!school.isPropertyLoaded("establishedDate")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "established_date"), "Mutation requires a fully loaded entity");
        }if(!school.isPropertyLoaded("studentCapacity")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "student_capacity"), "Mutation requires a fully loaded entity");
        }if(!school.isPropertyLoaded("active")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "active"), "Mutation requires a fully loaded entity");
        }if(!school.isPropertyLoaded("createTime")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "create_time"), "Mutation requires a fully loaded entity");
        }if(!school.isPropertyLoaded("updateTime")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "update_time"), "Mutation requires a fully loaded entity");
        }
      }
      checkPlatform(_context, school.getProperty(School.PLATFORM_PROPERTY), newLocation(_parentLocation, "platform"));
      checkSchoolType(_context, school.getProperty(School.SCHOOL_TYPE_PROPERTY), newLocation(_parentLocation, "school_type"));
      checkName(_context, school.getProperty(School.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      checkAddress(_context, school.getProperty(School.ADDRESS_PROPERTY), newLocation(_parentLocation, "address"));
      checkEstablishedDate(_context, school.getProperty(School.ESTABLISHED_DATE_PROPERTY), newLocation(_parentLocation, "established_date"));
      checkStudentCapacity(_context, school.getProperty(School.STUDENT_CAPACITY_PROPERTY), newLocation(_parentLocation, "student_capacity"));
      checkActive(_context, school.getProperty(School.ACTIVE_PROPERTY), newLocation(_parentLocation, "active"));
      checkCreateTime(_context, school.getProperty(School.CREATE_TIME_PROPERTY), newLocation(_parentLocation, "create_time"));
      checkUpdateTime(_context, school.getProperty(School.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, "update_time"));
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
    public void checkAddress(UserContext _context, String address, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, address);
    if((address == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, address);

    }
    public void checkEstablishedDate(UserContext _context, LocalDate establishedDate, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, establishedDate);
    if((establishedDate == null)){
        return;
    }
    }
    public void checkStudentCapacity(UserContext _context, Integer studentCapacity, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, studentCapacity);
    if((studentCapacity == null)){
        return;
    }
    }
    public void checkActive(UserContext _context, Boolean active, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, active);
    if((active == null)){
        return;
    }
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