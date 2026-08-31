
package com.example.schoolmanagementservice.platform;

import com.example.schoolmanagementservice.school.School;
import com.example.schoolmanagementservice.schooltype.SchoolType;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
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
public class Platform extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "Platform";

    public static final String NAME_PROPERTY = "name";
    public static final String BASE_URL_PROPERTY = "baseUrl";
    public static final String CREATE_TIME_PROPERTY = "createTime";
    public static final String UPDATE_TIME_PROPERTY = "updateTime";
    public static final String SCHOOL_TYPE_LIST_PROPERTY = "schoolTypeList";
    public static final String SCHOOL_LIST_PROPERTY = "schoolList";
    private String name;
    private String baseUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private SmartList<SchoolType> schoolTypeList;
    private SmartList<School> schoolList;

    public String getName(){
        return this.name;
    }
    public String getBaseUrl(){
        return this.baseUrl;
    }
    public LocalDateTime getCreateTime(){
        return this.createTime;
    }
    public LocalDateTime getUpdateTime(){
        return this.updateTime;
    }
    public SmartList<SchoolType> getSchoolTypeList(){
        return this.schoolTypeList;
    }
    public SmartList<School> getSchoolList(){
        return this.schoolList;
    }
    public Platform updateName(String name){
        name = (name == null ? null : name.trim());
        if(Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }
    public Platform updateBaseUrl(String baseUrl){
        baseUrl = (baseUrl == null ? null : baseUrl.trim());
        if(Objects.equals(this.baseUrl, baseUrl)){
            return this;
        }
        handleUpdate(BASE_URL_PROPERTY, getBaseUrl(), baseUrl);
        this.baseUrl = baseUrl;
        return this;
    }
    public Platform updateCreateTime(LocalDateTime createTime){
        if(Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }
    public Platform updateUpdateTime(LocalDateTime updateTime){
        if(Objects.equals(this.updateTime, updateTime)){
            return this;
        }
        handleUpdate(UPDATE_TIME_PROPERTY, getUpdateTime(), updateTime);
        this.updateTime = updateTime;
        return this;
    }
    public Platform addSchoolType(SchoolType schoolType){
        if (schoolType == null){
            return this;
        }

        if(null == this.schoolTypeList){
            this.schoolTypeList = new SmartList<>();
        }

        this.schoolTypeList.add(schoolType);
        schoolType.updatePlatform(this);
        return this;
    }
    public Platform addSchool(School school){
        if (school == null){
            return this;
        }

        if(null == this.schoolList){
            this.schoolList = new SmartList<>();
        }

        this.schoolList.add(school);
        school.updatePlatform(this);
        return this;
    }

    public static Platform refer(Long id){
        Platform refer = new Platform();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public Platform comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<Platform> auditAs(String action) {
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "baseUrl": this.baseUrl = (value == null ? null : ((String)value).trim()); break;

            case "createTime": this.createTime = (LocalDateTime) value; break;

            case "updateTime": this.updateTime = (LocalDateTime) value; break;

            case "schoolTypeList": this.schoolTypeList = (SmartList<SchoolType>) value; break;
            case "schoolList": this.schoolList = (SmartList<School>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "baseUrl": return this.baseUrl;
            case "createTime": return this.createTime;
            case "updateTime": return this.updateTime;
            case "schoolTypeList": return this.schoolTypeList;
            case "schoolList": return this.schoolList;
            default: return super.__internalGet(property);
        }
    }

}