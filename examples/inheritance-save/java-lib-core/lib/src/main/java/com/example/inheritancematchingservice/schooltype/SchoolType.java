
package com.example.inheritancematchingservice.schooltype;

import com.example.inheritancematchingservice.platform.Platform;
import com.example.inheritancematchingservice.school.School;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
import io.teaql.core.checker.JsonFieldNamingProfile;
import java.util.Objects;

/**
 * [TEAQL AI WARNING]
 * TeaQL was explicitly designed to PREVENT AI hallucinations and random guessing.
 * DO NOT GUESS METHOD NAMES!
 * The methods listed below are the ONLY valid ways to interact with this entity.
 * If you encounter compilation errors (e.g., method not found), DO NOT guess another method name.
 * Read the method signatures in this file before proceeding.
 */
public class SchoolType extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "SchoolType";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "595b434cfd28e9e4b60604e6092f6b709bf97fbb85753ad9f93443a203ae2e25";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("platform", 0), java.util.Map.entry("id", 1), java.util.Map.entry("name", 2), java.util.Map.entry("code", 3), java.util.Map.entry("version", 4));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("platform", java.util.List.of("platform", "platform")), java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("code", java.util.List.of("code", "code")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "school_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            SchoolType.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String PLATFORM_PROPERTY = "platform";
    public static final String NAME_PROPERTY = "name";
    public static final String CODE_PROPERTY = "code";
    public static final String SCHOOL_LIST_PROPERTY = "school_list";
    private Platform platform;

    private String name;

    private String code;

    private SmartList<School> schoolList;

    public Platform getPlatform(){
        return this.platform;
    }

    public String getName(){
        return this.name;
    }

    public String getCode(){
        return this.code;
    }

    public SmartList<School> getSchoolList(){
        return this.schoolList == null ? SmartList.unloaded() : this.schoolList;
    }
    public SchoolType updatePlatform(Platform platform){
        if(isPropertyLoaded(PLATFORM_PROPERTY) && Objects.equals(this.platform, platform)){
            return this;
        }
        handleUpdate(PLATFORM_PROPERTY, getPlatform(), platform);
        this.platform = platform;
        return this;
    }

    public SchoolType updateName(String name){
        name = (name == null ? null : name.trim());
        if(isPropertyLoaded(NAME_PROPERTY) && Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public SchoolType updateCode(String code){
        code = (code == null ? null : code.trim());
        if(isPropertyLoaded(CODE_PROPERTY) && Objects.equals(this.code, code)){
            return this;
        }
        handleUpdate(CODE_PROPERTY, getCode(), code);
        this.code = code;
        return this;
    }

    public SchoolType addSchool(School school){
        if (school == null){
            return this;
        }

        if(null == this.schoolList){
            SmartList<School> teaqlUnloadedItems = SmartList.unloaded();
            this.schoolList = teaqlUnloadedItems.map(value -> value);
        }

        this.schoolList.add(school);
        school.updateProperty(School.SCHOOL_TYPE_PROPERTY, this);
        return this;
    }

    public static SchoolType refer(Long id){
        SchoolType refer = new SchoolType();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public SchoolType comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<SchoolType> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "platform": this.platform = (Platform) value; break;

            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "code": this.code = (value == null ? null : ((String)value).trim()); break;

            case "school_list": this.schoolList = (SmartList<School>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "platform": return this.platform;
            case "name": return this.name;
            case "code": return this.code;
            case "school_list": return this.schoolList;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "platform": return this.platform;
                    case "name": return this.name;
                    case "code": return this.code;
                    default: return super.__internalGet(property);
                }
        }
    }

}