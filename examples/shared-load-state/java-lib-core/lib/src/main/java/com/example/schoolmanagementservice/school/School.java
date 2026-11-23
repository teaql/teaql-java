
package com.example.schoolmanagementservice.school;

import com.example.schoolmanagementservice.Constants;
import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.schooltype.SchoolType;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.checker.JsonFieldNamingProfile;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * [TEAQL AI WARNING]
 * TeaQL was explicitly designed to PREVENT AI hallucinations and random guessing.
 * DO NOT GUESS METHOD NAMES!
 * The methods listed below are the ONLY valid ways to interact with this entity.
 * If you encounter compilation errors (e.g., method not found), DO NOT guess another method name.
 * Read the method signatures in this file before proceeding.
 */
public class School extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "School";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "14b4bfbb4cda258779665e3bc38ab991040fe75793a416d29eddd06f8fa8f586";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("platform", 1), java.util.Map.entry("school_type", 2), java.util.Map.entry("name", 3), java.util.Map.entry("address", 4), java.util.Map.entry("established_date", 5), java.util.Map.entry("student_capacity", 6), java.util.Map.entry("active", 7), java.util.Map.entry("create_time", 8), java.util.Map.entry("update_time", 9), java.util.Map.entry("version", 10));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("platform", java.util.List.of("platform", "platform")), java.util.Map.entry("school_type", java.util.List.of("schoolType", "school_type")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("address", java.util.List.of("address", "address")), java.util.Map.entry("established_date", java.util.List.of("establishedDate", "established_date")), java.util.Map.entry("student_capacity", java.util.List.of("studentCapacity", "student_capacity")), java.util.Map.entry("active", java.util.List.of("active", "active")), java.util.Map.entry("create_time", java.util.List.of("createTime", "create_time")), java.util.Map.entry("update_time", java.util.List.of("updateTime", "update_time")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            School.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String PLATFORM_PROPERTY = "platform";
    public static final String SCHOOL_TYPE_PROPERTY = "school_type";
    public static final String NAME_PROPERTY = "name";
    public static final String ADDRESS_PROPERTY = "address";
    public static final String ESTABLISHED_DATE_PROPERTY = "established_date";
    public static final String STUDENT_CAPACITY_PROPERTY = "student_capacity";
    public static final String ACTIVE_PROPERTY = "active";
    public static final String CREATE_TIME_PROPERTY = "create_time";
    public static final String UPDATE_TIME_PROPERTY = "update_time";
    private Platform platform;

    private SchoolType schoolType;

    private String name;

    private String address;

    private LocalDate establishedDate;

    private Integer studentCapacity;

    private Boolean active;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


    public Platform getPlatform(){
        return this.platform;
    }

    public SchoolType getSchoolType(){
        return this.schoolType;
    }

    public String getName(){
        return this.name;
    }

    public String getAddress(){
        return this.address;
    }

    public LocalDate getEstablishedDate(){
        return this.establishedDate;
    }

    public Integer getStudentCapacity(){
        return this.studentCapacity;
    }

    public Boolean isActive(){
        return this.active;
    }

    public LocalDateTime getCreateTime(){
        return this.createTime;
    }

    public LocalDateTime getUpdateTime(){
        return this.updateTime;
    }

    public School updatePlatform(Platform platform){
        if(isPropertyLoaded(PLATFORM_PROPERTY) && Objects.equals(this.platform, platform)){
            return this;
        }
        handleUpdate(PLATFORM_PROPERTY, getPlatform(), platform);
        this.platform = platform;
        return this;
    }

    protected School updateSchoolType(SchoolType schoolType){
        if(isPropertyLoaded(SCHOOL_TYPE_PROPERTY) && Objects.equals(this.schoolType, schoolType)){
            return this;
        }
        handleUpdate(SCHOOL_TYPE_PROPERTY, getSchoolType(), schoolType);
        this.schoolType = schoolType;
        return this;
    }

    public School updateName(String name){
        name = (name == null ? null : name.trim());
        if(isPropertyLoaded(NAME_PROPERTY) && Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public School updateAddress(String address){
        address = (address == null ? null : address.trim());
        if(isPropertyLoaded(ADDRESS_PROPERTY) && Objects.equals(this.address, address)){
            return this;
        }
        handleUpdate(ADDRESS_PROPERTY, getAddress(), address);
        this.address = address;
        return this;
    }

    public School updateEstablishedDate(LocalDate establishedDate){
        if(isPropertyLoaded(ESTABLISHED_DATE_PROPERTY) && Objects.equals(this.establishedDate, establishedDate)){
            return this;
        }
        handleUpdate(ESTABLISHED_DATE_PROPERTY, getEstablishedDate(), establishedDate);
        this.establishedDate = establishedDate;
        return this;
    }

    public School updateStudentCapacity(Integer studentCapacity){
        if(isPropertyLoaded(STUDENT_CAPACITY_PROPERTY) && Objects.equals(this.studentCapacity, studentCapacity)){
            return this;
        }
        handleUpdate(STUDENT_CAPACITY_PROPERTY, getStudentCapacity(), studentCapacity);
        this.studentCapacity = studentCapacity;
        return this;
    }

    public School updateActive(Boolean active){
        if(isPropertyLoaded(ACTIVE_PROPERTY) && Objects.equals(this.active, active)){
            return this;
        }
        handleUpdate(ACTIVE_PROPERTY, isActive(), active);
        this.active = active;
        return this;
    }

    public School updateCreateTime(LocalDateTime createTime){
        if(isPropertyLoaded(CREATE_TIME_PROPERTY) && Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }

    public School updateUpdateTime(LocalDateTime updateTime){
        if(isPropertyLoaded(UPDATE_TIME_PROPERTY) && Objects.equals(this.updateTime, updateTime)){
            return this;
        }
        handleUpdate(UPDATE_TIME_PROPERTY, getUpdateTime(), updateTime);
        this.updateTime = updateTime;
        return this;
    }

    public boolean isSchoolTypePrimary(){
        return Objects.equals(getSchoolType(), Constants.SCHOOL_TYPE_PRIMARY);
    }

    public School updateSchoolTypeToPrimary(){
        return updateSchoolType(Constants.SCHOOL_TYPE_PRIMARY);
    }
    public boolean isSchoolTypeSecondary(){
        return Objects.equals(getSchoolType(), Constants.SCHOOL_TYPE_SECONDARY);
    }

    public School updateSchoolTypeToSecondary(){
        return updateSchoolType(Constants.SCHOOL_TYPE_SECONDARY);
    }

    public static School refer(Long id){
        School refer = new School();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public School comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<School> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "platform": this.platform = (Platform) value; break;

            case "school_type": this.schoolType = (SchoolType) value; break;

            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "address": this.address = (value == null ? null : ((String)value).trim()); break;

            case "established_date": this.establishedDate = (LocalDate) value; break;

            case "student_capacity": this.studentCapacity = (Integer) value; break;

            case "active": this.active = (Boolean) value; break;

            case "create_time": this.createTime = (LocalDateTime) value; break;

            case "update_time": this.updateTime = (LocalDateTime) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "platform": return this.platform;
            case "school_type": return this.schoolType;
            case "name": return this.name;
            case "address": return this.address;
            case "established_date": return this.establishedDate;
            case "student_capacity": return this.studentCapacity;
            case "active": return this.active;
            case "create_time": return this.createTime;
            case "update_time": return this.updateTime;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "platform": return this.platform;
                    case "schoolType": return this.schoolType;
                    case "name": return this.name;
                    case "address": return this.address;
                    case "establishedDate": return this.establishedDate;
                    case "studentCapacity": return this.studentCapacity;
                    case "active": return this.active;
                    case "createTime": return this.createTime;
                    case "updateTime": return this.updateTime;
                    default: return super.__internalGet(property);
                }
        }
    }

}