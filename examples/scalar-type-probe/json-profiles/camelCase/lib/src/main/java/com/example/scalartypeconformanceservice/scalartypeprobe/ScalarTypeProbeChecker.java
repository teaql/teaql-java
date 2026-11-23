
package com.example.scalartypeconformanceservice.scalartypeprobe;

import com.example.scalartypeconformanceservice.typeplatform.TypePlatform;
import com.example.scalartypeconformanceservice.typeplatform.TypePlatformChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ScalarTypeProbeChecker implements Checker<ScalarTypeProbe>{

    public String type(){
        return ScalarTypeProbe.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, ScalarTypeProbe scalarTypeProbe, ObjectLocation _parentLocation){
        if(needCheck(_context, scalarTypeProbe)){
            markAsChecked(_context, scalarTypeProbe);
            doCheck(_context, scalarTypeProbe, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, ScalarTypeProbe scalarTypeProbe, ObjectLocation _parentLocation){
      if((scalarTypeProbe == null)){
         return;
      }
      if(scalarTypeProbe.needPersist()){
      }
      if(scalarTypeProbe.newItem()){
        if(scalarTypeProbe.getCreateTime() == null){
           scalarTypeProbe.updateCreateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "create_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        }if(scalarTypeProbe.getUpdateTime() == null){
           scalarTypeProbe.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        }
      }else if(scalarTypeProbe.updateItem()){
        scalarTypeProbe.updateUpdateTime(_context.evaluate("now"));
        _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        if(!scalarTypeProbe.isPropertyLoaded("type_platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "type_platform"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("required_string")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "required_string"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("optional_string")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "optional_string"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("large_text")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "large_text"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("required_boolean")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "required_boolean"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("optional_boolean")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "optional_boolean"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("required_integer")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "required_integer"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("optional_integer")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "optional_integer"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("required_long")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "required_long"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("optional_long")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "optional_long"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("required_decimal")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "required_decimal"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("optional_decimal")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "optional_decimal"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("required_float")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "required_float"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("optional_float")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "optional_float"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("required_double")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "required_double"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("optional_double")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "optional_double"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("required_date")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "required_date"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("optional_date")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "optional_date"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("required_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "required_time"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("optional_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "optional_time"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("required_datetime")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "required_datetime"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("optional_datetime")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "optional_datetime"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("create_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "create_time"), "Mutation requires a fully loaded entity");
        }
        if(!scalarTypeProbe.isPropertyLoaded("update_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "update_time"), "Mutation requires a fully loaded entity");
        }

      }
      checkTypePlatform(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY), newLocation(_parentLocation, "type_platform"));
      checkRequiredString(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.REQUIRED_STRING_PROPERTY), newLocation(_parentLocation, "required_string"));
      checkOptionalString(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.OPTIONAL_STRING_PROPERTY), newLocation(_parentLocation, "optional_string"));
      checkLargeText(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.LARGE_TEXT_PROPERTY), newLocation(_parentLocation, "large_text"));
      checkRequiredBoolean(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY), newLocation(_parentLocation, "required_boolean"));
      checkOptionalBoolean(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.OPTIONAL_BOOLEAN_PROPERTY), newLocation(_parentLocation, "optional_boolean"));
      checkRequiredInteger(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY), newLocation(_parentLocation, "required_integer"));
      checkOptionalInteger(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY), newLocation(_parentLocation, "optional_integer"));
      checkRequiredLong(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.REQUIRED_LONG_PROPERTY), newLocation(_parentLocation, "required_long"));
      checkOptionalLong(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.OPTIONAL_LONG_PROPERTY), newLocation(_parentLocation, "optional_long"));
      checkRequiredDecimal(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY), newLocation(_parentLocation, "required_decimal"));
      checkOptionalDecimal(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY), newLocation(_parentLocation, "optional_decimal"));
      checkRequiredFloat(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY), newLocation(_parentLocation, "required_float"));
      checkOptionalFloat(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY), newLocation(_parentLocation, "optional_float"));
      checkRequiredDouble(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY), newLocation(_parentLocation, "required_double"));
      checkOptionalDouble(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY), newLocation(_parentLocation, "optional_double"));
      checkRequiredDate(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.REQUIRED_DATE_PROPERTY), newLocation(_parentLocation, "required_date"));
      checkOptionalDate(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.OPTIONAL_DATE_PROPERTY), newLocation(_parentLocation, "optional_date"));
      checkRequiredTime(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.REQUIRED_TIME_PROPERTY), newLocation(_parentLocation, "required_time"));
      checkOptionalTime(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.OPTIONAL_TIME_PROPERTY), newLocation(_parentLocation, "optional_time"));
      checkRequiredDatetime(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY), newLocation(_parentLocation, "required_datetime"));
      checkOptionalDatetime(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.OPTIONAL_DATETIME_PROPERTY), newLocation(_parentLocation, "optional_datetime"));
      checkCreateTime(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.CREATE_TIME_PROPERTY), newLocation(_parentLocation, "create_time"));
      checkUpdateTime(_context, scalarTypeProbe.getProperty(ScalarTypeProbe.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, "update_time"));
    }

    public void checkTypePlatform(UserContext _context, TypePlatform typePlatform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, typePlatform);
    if((typePlatform == null)){
        return;
    }
    new TypePlatformChecker().checkAndFix(_context, typePlatform, _parentLocation);
    }
    public void checkRequiredString(UserContext _context, String requiredString, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, requiredString);
    if((requiredString == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, requiredString);

    }
    public void checkOptionalString(UserContext _context, String optionalString, ObjectLocation _parentLocation){
    if((optionalString == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, optionalString);

    }
    public void checkLargeText(UserContext _context, String largeText, ObjectLocation _parentLocation){
    if((largeText == null)){
        return;
    }
    }
    public void checkRequiredBoolean(UserContext _context, Boolean requiredBoolean, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, requiredBoolean);
    if((requiredBoolean == null)){
        return;
    }
    }
    public void checkOptionalBoolean(UserContext _context, Boolean optionalBoolean, ObjectLocation _parentLocation){
    if((optionalBoolean == null)){
        return;
    }
    }
    public void checkRequiredInteger(UserContext _context, Integer requiredInteger, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, requiredInteger);
    if((requiredInteger == null)){
        return;
    }
    }
    public void checkOptionalInteger(UserContext _context, Integer optionalInteger, ObjectLocation _parentLocation){
    if((optionalInteger == null)){
        return;
    }
    }
    public void checkRequiredLong(UserContext _context, Long requiredLong, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, requiredLong);
    if((requiredLong == null)){
        return;
    }
    }
    public void checkOptionalLong(UserContext _context, Long optionalLong, ObjectLocation _parentLocation){
    if((optionalLong == null)){
        return;
    }
    }
    public void checkRequiredDecimal(UserContext _context, BigDecimal requiredDecimal, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, requiredDecimal);
    if((requiredDecimal == null)){
        return;
    }
    }
    public void checkOptionalDecimal(UserContext _context, BigDecimal optionalDecimal, ObjectLocation _parentLocation){
    if((optionalDecimal == null)){
        return;
    }
    }
    public void checkRequiredFloat(UserContext _context, Float requiredFloat, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, requiredFloat);
    if((requiredFloat == null)){
        return;
    }
    }
    public void checkOptionalFloat(UserContext _context, Float optionalFloat, ObjectLocation _parentLocation){
    if((optionalFloat == null)){
        return;
    }
    }
    public void checkRequiredDouble(UserContext _context, Double requiredDouble, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, requiredDouble);
    if((requiredDouble == null)){
        return;
    }
    }
    public void checkOptionalDouble(UserContext _context, Double optionalDouble, ObjectLocation _parentLocation){
    if((optionalDouble == null)){
        return;
    }
    }
    public void checkRequiredDate(UserContext _context, LocalDate requiredDate, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, requiredDate);
    if((requiredDate == null)){
        return;
    }
    }
    public void checkOptionalDate(UserContext _context, LocalDate optionalDate, ObjectLocation _parentLocation){
    if((optionalDate == null)){
        return;
    }
    }
    public void checkRequiredTime(UserContext _context, LocalTime requiredTime, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, requiredTime);
    if((requiredTime == null)){
        return;
    }
    }
    public void checkOptionalTime(UserContext _context, LocalTime optionalTime, ObjectLocation _parentLocation){
    if((optionalTime == null)){
        return;
    }
    }
    public void checkRequiredDatetime(UserContext _context, LocalDateTime requiredDatetime, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, requiredDatetime);
    if((requiredDatetime == null)){
        return;
    }
    }
    public void checkOptionalDatetime(UserContext _context, LocalDateTime optionalDatetime, ObjectLocation _parentLocation){
    if((optionalDatetime == null)){
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