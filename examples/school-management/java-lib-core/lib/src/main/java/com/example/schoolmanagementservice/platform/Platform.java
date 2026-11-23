
package com.example.schoolmanagementservice.platform;

import com.example.schoolmanagementservice.school.School;
import com.example.schoolmanagementservice.schooltype.SchoolType;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
import io.teaql.core.checker.JsonFieldNamingProfile;
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

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "e1da6639feab09f3d1ed370802410798c759f6bf13f85e0a93374c42d1646603";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("name", 1), java.util.Map.entry("base_url", 2), java.util.Map.entry("create_time", 3), java.util.Map.entry("update_time", 4), java.util.Map.entry("version", 5));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("base_url", java.util.List.of("baseUrl", "base_url")), java.util.Map.entry("create_time", java.util.List.of("createTime", "create_time")), java.util.Map.entry("update_time", java.util.List.of("updateTime", "update_time")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "school_type_list", "school_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            Platform.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String NAME_PROPERTY = "name";
    public static final String BASE_URL_PROPERTY = "base_url";
    public static final String CREATE_TIME_PROPERTY = "create_time";
    public static final String UPDATE_TIME_PROPERTY = "update_time";
    public static final String SCHOOL_TYPE_LIST_PROPERTY = "school_type_list";
    public static final String SCHOOL_LIST_PROPERTY = "school_list";
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
        return this.schoolTypeList == null ? SmartList.unloaded() : this.schoolTypeList;
    }
    public SmartList<School> getSchoolList(){
        return this.schoolList == null ? SmartList.unloaded() : this.schoolList;
    }
    public Platform updateName(String name){
        name = (name == null ? null : name.trim());
        if(isPropertyLoaded(NAME_PROPERTY) && Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public Platform updateBaseUrl(String baseUrl){
        baseUrl = (baseUrl == null ? null : baseUrl.trim());
        if(isPropertyLoaded(BASE_URL_PROPERTY) && Objects.equals(this.baseUrl, baseUrl)){
            return this;
        }
        handleUpdate(BASE_URL_PROPERTY, getBaseUrl(), baseUrl);
        this.baseUrl = baseUrl;
        return this;
    }

    public Platform updateCreateTime(LocalDateTime createTime){
        if(isPropertyLoaded(CREATE_TIME_PROPERTY) && Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }

    public Platform updateUpdateTime(LocalDateTime updateTime){
        if(isPropertyLoaded(UPDATE_TIME_PROPERTY) && Objects.equals(this.updateTime, updateTime)){
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
            SmartList<SchoolType> teaqlUnloadedItems = SmartList.unloaded();
            this.schoolTypeList = teaqlUnloadedItems.map(value -> value);
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
            SmartList<School> teaqlUnloadedItems = SmartList.unloaded();
            this.schoolList = teaqlUnloadedItems.map(value -> value);
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
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "base_url": this.baseUrl = (value == null ? null : ((String)value).trim()); break;

            case "create_time": this.createTime = (LocalDateTime) value; break;

            case "update_time": this.updateTime = (LocalDateTime) value; break;

            case "school_type_list": this.schoolTypeList = (SmartList<SchoolType>) value; break;
            case "school_list": this.schoolList = (SmartList<School>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "base_url": return this.baseUrl;
            case "create_time": return this.createTime;
            case "update_time": return this.updateTime;
            case "school_type_list": return this.schoolTypeList;
            case "school_list": return this.schoolList;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "name": return this.name;
                    case "baseUrl": return this.baseUrl;
                    case "createTime": return this.createTime;
                    case "updateTime": return this.updateTime;
                    default: return super.__internalGet(property);
                }
        }
    }

}