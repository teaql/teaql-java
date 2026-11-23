
package com.example.inheritancematchingservice.academy;

import com.example.inheritancematchingservice.platform.Platform;
import com.example.inheritancematchingservice.school.School;
import io.teaql.core.Audited;
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
public class Academy extends School implements RemoteInput {
    public static String INTERNAL_TYPE = "Academy";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "cd7f0bf3e8a453342d310c02c751feb21ed2383f4c58932743069e1be291b526";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("platform", 1), java.util.Map.entry("school_type", 2), java.util.Map.entry("name", 3), java.util.Map.entry("student_capacity", 4), java.util.Map.entry("version", 5), java.util.Map.entry("campus_code", 6));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("platform", java.util.List.of("platform", "platform")), java.util.Map.entry("school_type", java.util.List.of("schoolType", "school_type")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("student_capacity", java.util.List.of("studentCapacity", "student_capacity")), java.util.Map.entry("version", java.util.List.of("version", "version")), java.util.Map.entry("campus_code", java.util.List.of("campusCode", "Legacy Campus Code")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            Academy.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String CAMPUS_CODE_PROPERTY = "campus_code";
    private String campusCode;


    public String getCampusCode(){
        return this.campusCode;
    }

    public Academy updateCampusCode(String campusCode){
        campusCode = (campusCode == null ? null : campusCode.trim());
        if(isPropertyLoaded(CAMPUS_CODE_PROPERTY) && Objects.equals(this.campusCode, campusCode)){
            return this;
        }
        handleUpdate(CAMPUS_CODE_PROPERTY, getCampusCode(), campusCode);
        this.campusCode = campusCode;
        return this;
    }

    public Academy updatePlatform(Platform platform){
        super.updatePlatform(platform);
        return this;
    }

    public Academy updateSchoolTypeToPrimary(){
        super.updateSchoolTypeToPrimary();
        return this;
    }
    public Academy updateSchoolTypeToSecondary(){
        super.updateSchoolTypeToSecondary();
        return this;
    }

    public Academy updateName(String name){
        super.updateName(name);
        return this;
    }

    public Academy updateStudentCapacity(Integer studentCapacity){
        super.updateStudentCapacity(studentCapacity);
        return this;
    }


    public static Academy refer(Long id){
        Academy refer = new Academy();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public Academy comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<? extends Academy> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "campus_code": this.campusCode = (value == null ? null : ((String)value).trim()); break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "campus_code": return this.campusCode;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "campusCode": return this.campusCode;
                    default: return super.__internalGet(property);
                }
        }
    }

}