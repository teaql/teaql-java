
package com.example.scalartypeconformanceservice.scalartypeprobe;

import com.example.scalartypeconformanceservice.typeplatform.TypePlatform;
import com.example.scalartypeconformanceservice.typeplatform.TypePlatformExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Function;

public class ScalarTypeProbeExpression<T, E, U extends ScalarTypeProbe> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public ScalarTypeProbeExpression(Expression<T, U> expression){
        super(expression);
    }

    public ScalarTypeProbeExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public ScalarTypeProbeExpression<T, U, U> updateId(Long id){
        return new ScalarTypeProbeExpression(this, $it -> {((ScalarTypeProbe)$it).__internalSet("id", id); return this;});
     }

     public ScalarTypeProbeExpression<T, U, U> save(UserContext userContext){
        return new ScalarTypeProbeExpression(this, $it -> ((ScalarTypeProbe)$it).auditAs("Saved by Expression").save(userContext));
     }

     public ScalarTypeProbeExpression<T, U, U> save(String intent, UserContext userContext){
        return new ScalarTypeProbeExpression(this, $it -> ((ScalarTypeProbe)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public TypePlatformExpression<T, U, TypePlatform> getTypePlatform(){
       return new TypePlatformExpression(loaded("type_platform", "getTypePlatform", ScalarTypeProbe::getTypePlatform));
    }

    public ScalarTypeProbeExpression<T, U, U> updateTypePlatform(TypePlatform typePlatform){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateTypePlatform(typePlatform));
    }

    public Expression<T, String> getRequiredString(){
       return loaded("required_string", "getRequiredString", ScalarTypeProbe::getRequiredString);
    }
    public ScalarTypeProbeExpression<T, U, U> updateRequiredString(String requiredString){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateRequiredString(requiredString));
    }

    public Expression<T, String> getOptionalString(){
       return loaded("optional_string", "getOptionalString", ScalarTypeProbe::getOptionalString);
    }
    public ScalarTypeProbeExpression<T, U, U> updateOptionalString(String optionalString){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateOptionalString(optionalString));
    }

    public Expression<T, String> getLargeText(){
       return loaded("large_text", "getLargeText", ScalarTypeProbe::getLargeText);
    }
    public ScalarTypeProbeExpression<T, U, U> updateLargeText(String largeText){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateLargeText(largeText));
    }

    public Expression<T, Boolean> isRequiredBoolean(){
       return loaded("required_boolean", "isRequiredBoolean", ScalarTypeProbe::isRequiredBoolean);
    }
    public ScalarTypeProbeExpression<T, U, U> updateRequiredBoolean(Boolean requiredBoolean){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateRequiredBoolean(requiredBoolean));
    }

    public Expression<T, Boolean> isOptionalBoolean(){
       return loaded("optional_boolean", "isOptionalBoolean", ScalarTypeProbe::isOptionalBoolean);
    }
    public ScalarTypeProbeExpression<T, U, U> updateOptionalBoolean(Boolean optionalBoolean){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateOptionalBoolean(optionalBoolean));
    }

    public Expression<T, Integer> getRequiredInteger(){
       return loaded("required_integer", "getRequiredInteger", ScalarTypeProbe::getRequiredInteger);
    }
    public ScalarTypeProbeExpression<T, U, U> updateRequiredInteger(Integer requiredInteger){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateRequiredInteger(requiredInteger));
    }

    public Expression<T, Integer> getOptionalInteger(){
       return loaded("optional_integer", "getOptionalInteger", ScalarTypeProbe::getOptionalInteger);
    }
    public ScalarTypeProbeExpression<T, U, U> updateOptionalInteger(Integer optionalInteger){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateOptionalInteger(optionalInteger));
    }

    public Expression<T, Long> getRequiredLong(){
       return loaded("required_long", "getRequiredLong", ScalarTypeProbe::getRequiredLong);
    }
    public ScalarTypeProbeExpression<T, U, U> updateRequiredLong(Long requiredLong){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateRequiredLong(requiredLong));
    }

    public Expression<T, Long> getOptionalLong(){
       return loaded("optional_long", "getOptionalLong", ScalarTypeProbe::getOptionalLong);
    }
    public ScalarTypeProbeExpression<T, U, U> updateOptionalLong(Long optionalLong){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateOptionalLong(optionalLong));
    }

    public Expression<T, BigDecimal> getRequiredDecimal(){
       return loaded("required_decimal", "getRequiredDecimal", ScalarTypeProbe::getRequiredDecimal);
    }
    public ScalarTypeProbeExpression<T, U, U> updateRequiredDecimal(BigDecimal requiredDecimal){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateRequiredDecimal(requiredDecimal));
    }

    public Expression<T, BigDecimal> getOptionalDecimal(){
       return loaded("optional_decimal", "getOptionalDecimal", ScalarTypeProbe::getOptionalDecimal);
    }
    public ScalarTypeProbeExpression<T, U, U> updateOptionalDecimal(BigDecimal optionalDecimal){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateOptionalDecimal(optionalDecimal));
    }

    public Expression<T, Float> getRequiredFloat(){
       return loaded("required_float", "getRequiredFloat", ScalarTypeProbe::getRequiredFloat);
    }
    public ScalarTypeProbeExpression<T, U, U> updateRequiredFloat(Float requiredFloat){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateRequiredFloat(requiredFloat));
    }

    public Expression<T, Float> getOptionalFloat(){
       return loaded("optional_float", "getOptionalFloat", ScalarTypeProbe::getOptionalFloat);
    }
    public ScalarTypeProbeExpression<T, U, U> updateOptionalFloat(Float optionalFloat){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateOptionalFloat(optionalFloat));
    }

    public Expression<T, Double> getRequiredDouble(){
       return loaded("required_double", "getRequiredDouble", ScalarTypeProbe::getRequiredDouble);
    }
    public ScalarTypeProbeExpression<T, U, U> updateRequiredDouble(Double requiredDouble){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateRequiredDouble(requiredDouble));
    }

    public Expression<T, Double> getOptionalDouble(){
       return loaded("optional_double", "getOptionalDouble", ScalarTypeProbe::getOptionalDouble);
    }
    public ScalarTypeProbeExpression<T, U, U> updateOptionalDouble(Double optionalDouble){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateOptionalDouble(optionalDouble));
    }

    public Expression<T, LocalDate> getRequiredDate(){
       return loaded("required_date", "getRequiredDate", ScalarTypeProbe::getRequiredDate);
    }
    public ScalarTypeProbeExpression<T, U, U> updateRequiredDate(LocalDate requiredDate){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateRequiredDate(requiredDate));
    }

    public Expression<T, LocalDate> getOptionalDate(){
       return loaded("optional_date", "getOptionalDate", ScalarTypeProbe::getOptionalDate);
    }
    public ScalarTypeProbeExpression<T, U, U> updateOptionalDate(LocalDate optionalDate){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateOptionalDate(optionalDate));
    }

    public Expression<T, LocalTime> getRequiredTime(){
       return loaded("required_time", "getRequiredTime", ScalarTypeProbe::getRequiredTime);
    }
    public ScalarTypeProbeExpression<T, U, U> updateRequiredTime(LocalTime requiredTime){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateRequiredTime(requiredTime));
    }

    public Expression<T, LocalTime> getOptionalTime(){
       return loaded("optional_time", "getOptionalTime", ScalarTypeProbe::getOptionalTime);
    }
    public ScalarTypeProbeExpression<T, U, U> updateOptionalTime(LocalTime optionalTime){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateOptionalTime(optionalTime));
    }

    public Expression<T, LocalDateTime> getRequiredDatetime(){
       return loaded("required_datetime", "getRequiredDatetime", ScalarTypeProbe::getRequiredDatetime);
    }
    public ScalarTypeProbeExpression<T, U, U> updateRequiredDatetime(LocalDateTime requiredDatetime){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateRequiredDatetime(requiredDatetime));
    }

    public Expression<T, LocalDateTime> getOptionalDatetime(){
       return loaded("optional_datetime", "getOptionalDatetime", ScalarTypeProbe::getOptionalDatetime);
    }
    public ScalarTypeProbeExpression<T, U, U> updateOptionalDatetime(LocalDateTime optionalDatetime){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateOptionalDatetime(optionalDatetime));
    }

    public Expression<T, LocalDateTime> getCreateTime(){
       return loaded("create_time", "getCreateTime", ScalarTypeProbe::getCreateTime);
    }
    public ScalarTypeProbeExpression<T, U, U> updateCreateTime(LocalDateTime createTime){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateCreateTime(createTime));
    }

    public Expression<T, LocalDateTime> getUpdateTime(){
       return loaded("update_time", "getUpdateTime", ScalarTypeProbe::getUpdateTime);
    }
    public ScalarTypeProbeExpression<T, U, U> updateUpdateTime(LocalDateTime updateTime){
       return new ScalarTypeProbeExpression(this, $it ->  ((ScalarTypeProbe)$it).updateUpdateTime(updateTime));
    }

}