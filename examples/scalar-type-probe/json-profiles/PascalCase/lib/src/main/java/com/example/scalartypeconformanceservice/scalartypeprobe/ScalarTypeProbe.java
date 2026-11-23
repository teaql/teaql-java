
package com.example.scalartypeconformanceservice.scalartypeprobe;

import com.example.scalartypeconformanceservice.typeplatform.TypePlatform;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.checker.JsonFieldNamingProfile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * [TEAQL AI WARNING]
 * TeaQL was explicitly designed to PREVENT AI hallucinations and random guessing.
 * DO NOT GUESS METHOD NAMES!
 * The methods listed below are the ONLY valid ways to interact with this entity.
 * If you encounter compilation errors (e.g., method not found), DO NOT guess another method name.
 * Read the method signatures in this file before proceeding.
 */
public class ScalarTypeProbe extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "ScalarTypeProbe";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "8d1d53171c01628f604255bcfa7e7d52b6260332cee701781027b0233eae926b";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("type_platform", 1), java.util.Map.entry("required_string", 2), java.util.Map.entry("optional_string", 3), java.util.Map.entry("large_text", 4), java.util.Map.entry("required_boolean", 5), java.util.Map.entry("optional_boolean", 6), java.util.Map.entry("required_integer", 7), java.util.Map.entry("optional_integer", 8), java.util.Map.entry("required_long", 9), java.util.Map.entry("optional_long", 10), java.util.Map.entry("required_decimal", 11), java.util.Map.entry("optional_decimal", 12), java.util.Map.entry("required_float", 13), java.util.Map.entry("optional_float", 14), java.util.Map.entry("required_double", 15), java.util.Map.entry("optional_double", 16), java.util.Map.entry("required_date", 17), java.util.Map.entry("optional_date", 18), java.util.Map.entry("required_time", 19), java.util.Map.entry("optional_time", 20), java.util.Map.entry("required_datetime", 21), java.util.Map.entry("optional_datetime", 22), java.util.Map.entry("create_time", 23), java.util.Map.entry("update_time", 24), java.util.Map.entry("version", 25));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("type_platform", java.util.List.of("typePlatform", "type_platform")), java.util.Map.entry("required_string", java.util.List.of("requiredString", "required_string")), java.util.Map.entry("optional_string", java.util.List.of("optionalString", "optional_string")), java.util.Map.entry("large_text", java.util.List.of("largeText", "large_text")), java.util.Map.entry("required_boolean", java.util.List.of("requiredBoolean", "required_boolean")), java.util.Map.entry("optional_boolean", java.util.List.of("optionalBoolean", "optional_boolean")), java.util.Map.entry("required_integer", java.util.List.of("requiredInteger", "required_integer")), java.util.Map.entry("optional_integer", java.util.List.of("optionalInteger", "optional_integer")), java.util.Map.entry("required_long", java.util.List.of("requiredLong", "required_long")), java.util.Map.entry("optional_long", java.util.List.of("optionalLong", "optional_long")), java.util.Map.entry("required_decimal", java.util.List.of("requiredDecimal", "required_decimal")), java.util.Map.entry("optional_decimal", java.util.List.of("optionalDecimal", "optional_decimal")), java.util.Map.entry("required_float", java.util.List.of("requiredFloat", "required_float")), java.util.Map.entry("optional_float", java.util.List.of("optionalFloat", "optional_float")), java.util.Map.entry("required_double", java.util.List.of("requiredDouble", "required_double")), java.util.Map.entry("optional_double", java.util.List.of("optionalDouble", "optional_double")), java.util.Map.entry("required_date", java.util.List.of("requiredDate", "required_date")), java.util.Map.entry("optional_date", java.util.List.of("optionalDate", "optional_date")), java.util.Map.entry("required_time", java.util.List.of("requiredTime", "required_time")), java.util.Map.entry("optional_time", java.util.List.of("optionalTime", "optional_time")), java.util.Map.entry("required_datetime", java.util.List.of("requiredDatetime", "required_datetime")), java.util.Map.entry("optional_datetime", java.util.List.of("optionalDatetime", "optional_datetime")), java.util.Map.entry("create_time", java.util.List.of("createTime", "create_time")), java.util.Map.entry("update_time", java.util.List.of("updateTime", "update_time")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            ScalarTypeProbe.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("PascalCase")));


    public static final String TYPE_PLATFORM_PROPERTY = "type_platform";
    public static final String REQUIRED_STRING_PROPERTY = "required_string";
    public static final String OPTIONAL_STRING_PROPERTY = "optional_string";
    public static final String LARGE_TEXT_PROPERTY = "large_text";
    public static final String REQUIRED_BOOLEAN_PROPERTY = "required_boolean";
    public static final String OPTIONAL_BOOLEAN_PROPERTY = "optional_boolean";
    public static final String REQUIRED_INTEGER_PROPERTY = "required_integer";
    public static final String OPTIONAL_INTEGER_PROPERTY = "optional_integer";
    public static final String REQUIRED_LONG_PROPERTY = "required_long";
    public static final String OPTIONAL_LONG_PROPERTY = "optional_long";
    public static final String REQUIRED_DECIMAL_PROPERTY = "required_decimal";
    public static final String OPTIONAL_DECIMAL_PROPERTY = "optional_decimal";
    public static final String REQUIRED_FLOAT_PROPERTY = "required_float";
    public static final String OPTIONAL_FLOAT_PROPERTY = "optional_float";
    public static final String REQUIRED_DOUBLE_PROPERTY = "required_double";
    public static final String OPTIONAL_DOUBLE_PROPERTY = "optional_double";
    public static final String REQUIRED_DATE_PROPERTY = "required_date";
    public static final String OPTIONAL_DATE_PROPERTY = "optional_date";
    public static final String REQUIRED_TIME_PROPERTY = "required_time";
    public static final String OPTIONAL_TIME_PROPERTY = "optional_time";
    public static final String REQUIRED_DATETIME_PROPERTY = "required_datetime";
    public static final String OPTIONAL_DATETIME_PROPERTY = "optional_datetime";
    public static final String CREATE_TIME_PROPERTY = "create_time";
    public static final String UPDATE_TIME_PROPERTY = "update_time";
    private TypePlatform typePlatform;

    private String requiredString;

    private String optionalString;

    private String largeText;

    private Boolean requiredBoolean;

    private Boolean optionalBoolean;

    private Integer requiredInteger;

    private Integer optionalInteger;

    private Long requiredLong;

    private Long optionalLong;

    private BigDecimal requiredDecimal;

    private BigDecimal optionalDecimal;

    private Float requiredFloat;

    private Float optionalFloat;

    private Double requiredDouble;

    private Double optionalDouble;

    private LocalDate requiredDate;

    private LocalDate optionalDate;

    private LocalTime requiredTime;

    private LocalTime optionalTime;

    private LocalDateTime requiredDatetime;

    private LocalDateTime optionalDatetime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


    public TypePlatform getTypePlatform(){
        return this.typePlatform;
    }

    public String getRequiredString(){
        return this.requiredString;
    }

    public String getOptionalString(){
        return this.optionalString;
    }

    public String getLargeText(){
        return this.largeText;
    }

    public Boolean isRequiredBoolean(){
        return this.requiredBoolean;
    }

    public Boolean isOptionalBoolean(){
        return this.optionalBoolean;
    }

    public Integer getRequiredInteger(){
        return this.requiredInteger;
    }

    public Integer getOptionalInteger(){
        return this.optionalInteger;
    }

    public Long getRequiredLong(){
        return this.requiredLong;
    }

    public Long getOptionalLong(){
        return this.optionalLong;
    }

    public BigDecimal getRequiredDecimal(){
        return this.requiredDecimal;
    }

    public BigDecimal getOptionalDecimal(){
        return this.optionalDecimal;
    }

    public Float getRequiredFloat(){
        return this.requiredFloat;
    }

    public Float getOptionalFloat(){
        return this.optionalFloat;
    }

    public Double getRequiredDouble(){
        return this.requiredDouble;
    }

    public Double getOptionalDouble(){
        return this.optionalDouble;
    }

    public LocalDate getRequiredDate(){
        return this.requiredDate;
    }

    public LocalDate getOptionalDate(){
        return this.optionalDate;
    }

    public LocalTime getRequiredTime(){
        return this.requiredTime;
    }

    public LocalTime getOptionalTime(){
        return this.optionalTime;
    }

    public LocalDateTime getRequiredDatetime(){
        return this.requiredDatetime;
    }

    public LocalDateTime getOptionalDatetime(){
        return this.optionalDatetime;
    }

    public LocalDateTime getCreateTime(){
        return this.createTime;
    }

    public LocalDateTime getUpdateTime(){
        return this.updateTime;
    }

    public ScalarTypeProbe updateTypePlatform(TypePlatform typePlatform){
        if(isPropertyLoaded(TYPE_PLATFORM_PROPERTY) && Objects.equals(this.typePlatform, typePlatform)){
            return this;
        }
        handleUpdate(TYPE_PLATFORM_PROPERTY, getTypePlatform(), typePlatform);
        this.typePlatform = typePlatform;
        return this;
    }

    public ScalarTypeProbe updateRequiredString(String requiredString){
        requiredString = (requiredString == null ? null : requiredString.trim());
        if(isPropertyLoaded(REQUIRED_STRING_PROPERTY) && Objects.equals(this.requiredString, requiredString)){
            return this;
        }
        handleUpdate(REQUIRED_STRING_PROPERTY, getRequiredString(), requiredString);
        this.requiredString = requiredString;
        return this;
    }

    public ScalarTypeProbe updateOptionalString(String optionalString){
        optionalString = (optionalString == null ? null : optionalString.trim());
        if(isPropertyLoaded(OPTIONAL_STRING_PROPERTY) && Objects.equals(this.optionalString, optionalString)){
            return this;
        }
        handleUpdate(OPTIONAL_STRING_PROPERTY, getOptionalString(), optionalString);
        this.optionalString = optionalString;
        return this;
    }

    public ScalarTypeProbe updateLargeText(String largeText){
        if(isPropertyLoaded(LARGE_TEXT_PROPERTY) && Objects.equals(this.largeText, largeText)){
            return this;
        }
        handleUpdate(LARGE_TEXT_PROPERTY, getLargeText(), largeText);
        this.largeText = largeText;
        return this;
    }

    public ScalarTypeProbe updateRequiredBoolean(Boolean requiredBoolean){
        if(isPropertyLoaded(REQUIRED_BOOLEAN_PROPERTY) && Objects.equals(this.requiredBoolean, requiredBoolean)){
            return this;
        }
        handleUpdate(REQUIRED_BOOLEAN_PROPERTY, isRequiredBoolean(), requiredBoolean);
        this.requiredBoolean = requiredBoolean;
        return this;
    }

    public ScalarTypeProbe updateOptionalBoolean(Boolean optionalBoolean){
        if(isPropertyLoaded(OPTIONAL_BOOLEAN_PROPERTY) && Objects.equals(this.optionalBoolean, optionalBoolean)){
            return this;
        }
        handleUpdate(OPTIONAL_BOOLEAN_PROPERTY, isOptionalBoolean(), optionalBoolean);
        this.optionalBoolean = optionalBoolean;
        return this;
    }

    public ScalarTypeProbe updateRequiredInteger(Integer requiredInteger){
        if(isPropertyLoaded(REQUIRED_INTEGER_PROPERTY) && Objects.equals(this.requiredInteger, requiredInteger)){
            return this;
        }
        handleUpdate(REQUIRED_INTEGER_PROPERTY, getRequiredInteger(), requiredInteger);
        this.requiredInteger = requiredInteger;
        return this;
    }

    public ScalarTypeProbe updateOptionalInteger(Integer optionalInteger){
        if(isPropertyLoaded(OPTIONAL_INTEGER_PROPERTY) && Objects.equals(this.optionalInteger, optionalInteger)){
            return this;
        }
        handleUpdate(OPTIONAL_INTEGER_PROPERTY, getOptionalInteger(), optionalInteger);
        this.optionalInteger = optionalInteger;
        return this;
    }

    public ScalarTypeProbe updateRequiredLong(Long requiredLong){
        if(isPropertyLoaded(REQUIRED_LONG_PROPERTY) && Objects.equals(this.requiredLong, requiredLong)){
            return this;
        }
        handleUpdate(REQUIRED_LONG_PROPERTY, getRequiredLong(), requiredLong);
        this.requiredLong = requiredLong;
        return this;
    }

    public ScalarTypeProbe updateOptionalLong(Long optionalLong){
        if(isPropertyLoaded(OPTIONAL_LONG_PROPERTY) && Objects.equals(this.optionalLong, optionalLong)){
            return this;
        }
        handleUpdate(OPTIONAL_LONG_PROPERTY, getOptionalLong(), optionalLong);
        this.optionalLong = optionalLong;
        return this;
    }

    public ScalarTypeProbe updateRequiredDecimal(BigDecimal requiredDecimal){
        if(isPropertyLoaded(REQUIRED_DECIMAL_PROPERTY) && Objects.equals(this.requiredDecimal, requiredDecimal)){
            return this;
        }
        handleUpdate(REQUIRED_DECIMAL_PROPERTY, getRequiredDecimal(), requiredDecimal);
        this.requiredDecimal = requiredDecimal;
        return this;
    }

    public ScalarTypeProbe updateOptionalDecimal(BigDecimal optionalDecimal){
        if(isPropertyLoaded(OPTIONAL_DECIMAL_PROPERTY) && Objects.equals(this.optionalDecimal, optionalDecimal)){
            return this;
        }
        handleUpdate(OPTIONAL_DECIMAL_PROPERTY, getOptionalDecimal(), optionalDecimal);
        this.optionalDecimal = optionalDecimal;
        return this;
    }

    public ScalarTypeProbe updateRequiredFloat(Float requiredFloat){
        if(isPropertyLoaded(REQUIRED_FLOAT_PROPERTY) && Objects.equals(this.requiredFloat, requiredFloat)){
            return this;
        }
        handleUpdate(REQUIRED_FLOAT_PROPERTY, getRequiredFloat(), requiredFloat);
        this.requiredFloat = requiredFloat;
        return this;
    }

    public ScalarTypeProbe updateOptionalFloat(Float optionalFloat){
        if(isPropertyLoaded(OPTIONAL_FLOAT_PROPERTY) && Objects.equals(this.optionalFloat, optionalFloat)){
            return this;
        }
        handleUpdate(OPTIONAL_FLOAT_PROPERTY, getOptionalFloat(), optionalFloat);
        this.optionalFloat = optionalFloat;
        return this;
    }

    public ScalarTypeProbe updateRequiredDouble(Double requiredDouble){
        if(isPropertyLoaded(REQUIRED_DOUBLE_PROPERTY) && Objects.equals(this.requiredDouble, requiredDouble)){
            return this;
        }
        handleUpdate(REQUIRED_DOUBLE_PROPERTY, getRequiredDouble(), requiredDouble);
        this.requiredDouble = requiredDouble;
        return this;
    }

    public ScalarTypeProbe updateOptionalDouble(Double optionalDouble){
        if(isPropertyLoaded(OPTIONAL_DOUBLE_PROPERTY) && Objects.equals(this.optionalDouble, optionalDouble)){
            return this;
        }
        handleUpdate(OPTIONAL_DOUBLE_PROPERTY, getOptionalDouble(), optionalDouble);
        this.optionalDouble = optionalDouble;
        return this;
    }

    public ScalarTypeProbe updateRequiredDate(LocalDate requiredDate){
        if(isPropertyLoaded(REQUIRED_DATE_PROPERTY) && Objects.equals(this.requiredDate, requiredDate)){
            return this;
        }
        handleUpdate(REQUIRED_DATE_PROPERTY, getRequiredDate(), requiredDate);
        this.requiredDate = requiredDate;
        return this;
    }

    public ScalarTypeProbe updateOptionalDate(LocalDate optionalDate){
        if(isPropertyLoaded(OPTIONAL_DATE_PROPERTY) && Objects.equals(this.optionalDate, optionalDate)){
            return this;
        }
        handleUpdate(OPTIONAL_DATE_PROPERTY, getOptionalDate(), optionalDate);
        this.optionalDate = optionalDate;
        return this;
    }

    public ScalarTypeProbe updateRequiredTime(LocalTime requiredTime){
        if(isPropertyLoaded(REQUIRED_TIME_PROPERTY) && Objects.equals(this.requiredTime, requiredTime)){
            return this;
        }
        handleUpdate(REQUIRED_TIME_PROPERTY, getRequiredTime(), requiredTime);
        this.requiredTime = requiredTime;
        return this;
    }

    public ScalarTypeProbe updateOptionalTime(LocalTime optionalTime){
        if(isPropertyLoaded(OPTIONAL_TIME_PROPERTY) && Objects.equals(this.optionalTime, optionalTime)){
            return this;
        }
        handleUpdate(OPTIONAL_TIME_PROPERTY, getOptionalTime(), optionalTime);
        this.optionalTime = optionalTime;
        return this;
    }

    public ScalarTypeProbe updateRequiredDatetime(LocalDateTime requiredDatetime){
        if(isPropertyLoaded(REQUIRED_DATETIME_PROPERTY) && Objects.equals(this.requiredDatetime, requiredDatetime)){
            return this;
        }
        handleUpdate(REQUIRED_DATETIME_PROPERTY, getRequiredDatetime(), requiredDatetime);
        this.requiredDatetime = requiredDatetime;
        return this;
    }

    public ScalarTypeProbe updateOptionalDatetime(LocalDateTime optionalDatetime){
        if(isPropertyLoaded(OPTIONAL_DATETIME_PROPERTY) && Objects.equals(this.optionalDatetime, optionalDatetime)){
            return this;
        }
        handleUpdate(OPTIONAL_DATETIME_PROPERTY, getOptionalDatetime(), optionalDatetime);
        this.optionalDatetime = optionalDatetime;
        return this;
    }

    public ScalarTypeProbe updateCreateTime(LocalDateTime createTime){
        if(isPropertyLoaded(CREATE_TIME_PROPERTY) && Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }

    public ScalarTypeProbe updateUpdateTime(LocalDateTime updateTime){
        if(isPropertyLoaded(UPDATE_TIME_PROPERTY) && Objects.equals(this.updateTime, updateTime)){
            return this;
        }
        handleUpdate(UPDATE_TIME_PROPERTY, getUpdateTime(), updateTime);
        this.updateTime = updateTime;
        return this;
    }


    public static ScalarTypeProbe refer(Long id){
        ScalarTypeProbe refer = new ScalarTypeProbe();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public ScalarTypeProbe comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<ScalarTypeProbe> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "type_platform": this.typePlatform = (TypePlatform) value; break;

            case "required_string": this.requiredString = (value == null ? null : ((String)value).trim()); break;

            case "optional_string": this.optionalString = (value == null ? null : ((String)value).trim()); break;

            case "large_text": this.largeText = (String) value; break;

            case "required_boolean": this.requiredBoolean = (Boolean) value; break;

            case "optional_boolean": this.optionalBoolean = (Boolean) value; break;

            case "required_integer": this.requiredInteger = (Integer) value; break;

            case "optional_integer": this.optionalInteger = (Integer) value; break;

            case "required_long": this.requiredLong = (Long) value; break;

            case "optional_long": this.optionalLong = (Long) value; break;

            case "required_decimal": this.requiredDecimal = (BigDecimal) value; break;

            case "optional_decimal": this.optionalDecimal = (BigDecimal) value; break;

            case "required_float": this.requiredFloat = (Float) value; break;

            case "optional_float": this.optionalFloat = (Float) value; break;

            case "required_double": this.requiredDouble = (Double) value; break;

            case "optional_double": this.optionalDouble = (Double) value; break;

            case "required_date": this.requiredDate = (LocalDate) value; break;

            case "optional_date": this.optionalDate = (LocalDate) value; break;

            case "required_time": this.requiredTime = (LocalTime) value; break;

            case "optional_time": this.optionalTime = (LocalTime) value; break;

            case "required_datetime": this.requiredDatetime = (LocalDateTime) value; break;

            case "optional_datetime": this.optionalDatetime = (LocalDateTime) value; break;

            case "create_time": this.createTime = (LocalDateTime) value; break;

            case "update_time": this.updateTime = (LocalDateTime) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "type_platform": return this.typePlatform;
            case "required_string": return this.requiredString;
            case "optional_string": return this.optionalString;
            case "large_text": return this.largeText;
            case "required_boolean": return this.requiredBoolean;
            case "optional_boolean": return this.optionalBoolean;
            case "required_integer": return this.requiredInteger;
            case "optional_integer": return this.optionalInteger;
            case "required_long": return this.requiredLong;
            case "optional_long": return this.optionalLong;
            case "required_decimal": return this.requiredDecimal;
            case "optional_decimal": return this.optionalDecimal;
            case "required_float": return this.requiredFloat;
            case "optional_float": return this.optionalFloat;
            case "required_double": return this.requiredDouble;
            case "optional_double": return this.optionalDouble;
            case "required_date": return this.requiredDate;
            case "optional_date": return this.optionalDate;
            case "required_time": return this.requiredTime;
            case "optional_time": return this.optionalTime;
            case "required_datetime": return this.requiredDatetime;
            case "optional_datetime": return this.optionalDatetime;
            case "create_time": return this.createTime;
            case "update_time": return this.updateTime;
            default: return super.__internalGet(property);
        }
    }

}