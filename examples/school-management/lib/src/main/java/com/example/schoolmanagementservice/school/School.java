
package com.example.schoolmanagementservice.school;

import com.example.schoolmanagementservice.Constants;
import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.schooltype.SchoolType;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
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

    public static final String PLATFORM_PROPERTY = "platform";
    public static final String SCHOOL_TYPE_PROPERTY = "schoolType";
    public static final String NAME_PROPERTY = "name";
    public static final String ADDRESS_PROPERTY = "address";
    public static final String ESTABLISHED_DATE_PROPERTY = "establishedDate";
    public static final String STUDENT_CAPACITY_PROPERTY = "studentCapacity";
    public static final String ACTIVE_PROPERTY = "active";
    public static final String CREATE_TIME_PROPERTY = "createTime";
    public static final String UPDATE_TIME_PROPERTY = "updateTime";
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
        if(Objects.equals(this.platform, platform)){
            return this;
        }
        handleUpdate(PLATFORM_PROPERTY, getPlatform(), platform);
        this.platform = platform;
        return this;
    }
    protected School updateSchoolType(SchoolType schoolType){
        if(Objects.equals(this.schoolType, schoolType)){
            return this;
        }
        handleUpdate(SCHOOL_TYPE_PROPERTY, getSchoolType(), schoolType);
        this.schoolType = schoolType;
        return this;
    }
    public School updateName(String name){
        name = (name == null ? null : name.trim());
        if(Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }
    public School updateAddress(String address){
        address = (address == null ? null : address.trim());
        if(Objects.equals(this.address, address)){
            return this;
        }
        handleUpdate(ADDRESS_PROPERTY, getAddress(), address);
        this.address = address;
        return this;
    }
    public School updateEstablishedDate(LocalDate establishedDate){
        if(Objects.equals(this.establishedDate, establishedDate)){
            return this;
        }
        handleUpdate(ESTABLISHED_DATE_PROPERTY, getEstablishedDate(), establishedDate);
        this.establishedDate = establishedDate;
        return this;
    }
    public School updateStudentCapacity(Integer studentCapacity){
        if(Objects.equals(this.studentCapacity, studentCapacity)){
            return this;
        }
        handleUpdate(STUDENT_CAPACITY_PROPERTY, getStudentCapacity(), studentCapacity);
        this.studentCapacity = studentCapacity;
        return this;
    }
    public School updateActive(Boolean active){
        if(Objects.equals(this.active, active)){
            return this;
        }
        handleUpdate(ACTIVE_PROPERTY, isActive(), active);
        this.active = active;
        return this;
    }
    public School updateCreateTime(LocalDateTime createTime){
        if(Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }
    public School updateUpdateTime(LocalDateTime updateTime){
        if(Objects.equals(this.updateTime, updateTime)){
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
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "platform": this.platform = (Platform) value; break;

            case "schoolType": this.schoolType = (SchoolType) value; break;

            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "address": this.address = (value == null ? null : ((String)value).trim()); break;

            case "establishedDate": this.establishedDate = (LocalDate) value; break;

            case "studentCapacity": this.studentCapacity = (Integer) value; break;

            case "active": this.active = (Boolean) value; break;

            case "createTime": this.createTime = (LocalDateTime) value; break;

            case "updateTime": this.updateTime = (LocalDateTime) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
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