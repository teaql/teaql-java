
package com.example.schoolmanagementservice.schoolcapacitysummary;

import com.example.schoolmanagementservice.platform.Platform;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
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
public class SchoolCapacitySummary extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "SchoolCapacitySummary";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "f571f4dcc37bd33fc29f0aa97e881485bf74a0130eef2c5230fe6c7e15c257f4";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("platform", 1), java.util.Map.entry("name", 2), java.util.Map.entry("total_capacity", 3), java.util.Map.entry("school_count", 4), java.util.Map.entry("version", 5));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("platform", java.util.List.of("platform", "platform")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("total_capacity", java.util.List.of("totalCapacity", "total_capacity")), java.util.Map.entry("school_count", java.util.List.of("schoolCount", "school_count")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            SchoolCapacitySummary.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String PLATFORM_PROPERTY = "platform";
    public static final String NAME_PROPERTY = "name";
    public static final String TOTAL_CAPACITY_PROPERTY = "total_capacity";
    public static final String SCHOOL_COUNT_PROPERTY = "school_count";
    private Platform platform;

    private String name;

    private Integer totalCapacity;

    private Integer schoolCount;


    public Platform getPlatform(){
        return this.platform;
    }

    public String getName(){
        return this.name;
    }

    public Integer getTotalCapacity(){
        return this.totalCapacity;
    }

    public Integer getSchoolCount(){
        return this.schoolCount;
    }

    public SchoolCapacitySummary updatePlatform(Platform platform){
        if(isPropertyLoaded(PLATFORM_PROPERTY) && Objects.equals(this.platform, platform)){
            return this;
        }
        handleUpdate(PLATFORM_PROPERTY, getPlatform(), platform);
        this.platform = platform;
        return this;
    }

    public SchoolCapacitySummary updateName(String name){
        name = (name == null ? null : name.trim());
        if(isPropertyLoaded(NAME_PROPERTY) && Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public SchoolCapacitySummary updateTotalCapacity(Integer totalCapacity){
        if(isPropertyLoaded(TOTAL_CAPACITY_PROPERTY) && Objects.equals(this.totalCapacity, totalCapacity)){
            return this;
        }
        handleUpdate(TOTAL_CAPACITY_PROPERTY, getTotalCapacity(), totalCapacity);
        this.totalCapacity = totalCapacity;
        return this;
    }

    public SchoolCapacitySummary updateSchoolCount(Integer schoolCount){
        if(isPropertyLoaded(SCHOOL_COUNT_PROPERTY) && Objects.equals(this.schoolCount, schoolCount)){
            return this;
        }
        handleUpdate(SCHOOL_COUNT_PROPERTY, getSchoolCount(), schoolCount);
        this.schoolCount = schoolCount;
        return this;
    }


    public static SchoolCapacitySummary refer(Long id){
        SchoolCapacitySummary refer = new SchoolCapacitySummary();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public SchoolCapacitySummary comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<SchoolCapacitySummary> auditAs(String action) {
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

            case "total_capacity": this.totalCapacity = (Integer) value; break;

            case "school_count": this.schoolCount = (Integer) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "platform": return this.platform;
            case "name": return this.name;
            case "total_capacity": return this.totalCapacity;
            case "school_count": return this.schoolCount;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "platform": return this.platform;
                    case "name": return this.name;
                    case "totalCapacity": return this.totalCapacity;
                    case "schoolCount": return this.schoolCount;
                    default: return super.__internalGet(property);
                }
        }
    }

}