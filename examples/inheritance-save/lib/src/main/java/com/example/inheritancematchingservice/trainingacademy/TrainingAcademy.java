
package com.example.inheritancematchingservice.trainingacademy;

import com.example.inheritancematchingservice.academy.Academy;
import com.example.inheritancematchingservice.platform.Platform;
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
public class TrainingAcademy extends Academy implements RemoteInput {
    public static String INTERNAL_TYPE = "TrainingAcademy";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "febc19f1547b3c9d17b9acd2ae34f398323bbe38ad635615cf4c19a534590fbc";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("platform", 1), java.util.Map.entry("school_type", 2), java.util.Map.entry("name", 3), java.util.Map.entry("student_capacity", 4), java.util.Map.entry("version", 5), java.util.Map.entry("campus_code", 6), java.util.Map.entry("specialty", 7));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("platform", java.util.List.of("platform", "platform")), java.util.Map.entry("school_type", java.util.List.of("schoolType", "school_type")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("student_capacity", java.util.List.of("studentCapacity", "student_capacity")), java.util.Map.entry("version", java.util.List.of("version", "version")), java.util.Map.entry("campus_code", java.util.List.of("campusCode", "Legacy Campus Code")), java.util.Map.entry("specialty", java.util.List.of("specialty", "specialty")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            TrainingAcademy.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String SPECIALTY_PROPERTY = "specialty";
    private String specialty;


    public String getSpecialty(){
        return this.specialty;
    }

    public TrainingAcademy updateSpecialty(String specialty){
        specialty = (specialty == null ? null : specialty.trim());
        if(isPropertyLoaded(SPECIALTY_PROPERTY) && Objects.equals(this.specialty, specialty)){
            return this;
        }
        handleUpdate(SPECIALTY_PROPERTY, getSpecialty(), specialty);
        this.specialty = specialty;
        return this;
    }

    public TrainingAcademy updateCampusCode(String campusCode){
        super.updateCampusCode(campusCode);
        return this;
    }

    public TrainingAcademy updatePlatform(Platform platform){
        super.updatePlatform(platform);
        return this;
    }

    public TrainingAcademy updateSchoolTypeToPrimary(){
        super.updateSchoolTypeToPrimary();
        return this;
    }
    public TrainingAcademy updateSchoolTypeToSecondary(){
        super.updateSchoolTypeToSecondary();
        return this;
    }

    public TrainingAcademy updateName(String name){
        super.updateName(name);
        return this;
    }

    public TrainingAcademy updateStudentCapacity(Integer studentCapacity){
        super.updateStudentCapacity(studentCapacity);
        return this;
    }


    public static TrainingAcademy refer(Long id){
        TrainingAcademy refer = new TrainingAcademy();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public TrainingAcademy comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<TrainingAcademy> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "specialty": this.specialty = (value == null ? null : ((String)value).trim()); break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "specialty": return this.specialty;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "specialty": return this.specialty;
                    default: return super.__internalGet(property);
                }
        }
    }

}