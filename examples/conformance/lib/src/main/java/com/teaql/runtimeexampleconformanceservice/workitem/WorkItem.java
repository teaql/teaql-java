
package com.teaql.runtimeexampleconformanceservice.workitem;

import com.teaql.runtimeexampleconformanceservice.platform.Platform;
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
public class WorkItem extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "WorkItem";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "ff6df6930613256f676f63204201b7b20d7ecd9fa363bae51492c66e562f023c";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("title", 1), java.util.Map.entry("description", 2), java.util.Map.entry("platform", 3), java.util.Map.entry("version", 4));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("title", java.util.List.of("title", "title")), java.util.Map.entry("description", java.util.List.of("description", "description")), java.util.Map.entry("platform", java.util.List.of("platform", "platform")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            WorkItem.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String TITLE_PROPERTY = "title";
    public static final String DESCRIPTION_PROPERTY = "description";
    public static final String PLATFORM_PROPERTY = "platform";
    private String title;

    private String description;

    private Platform platform;


    public String getTitle(){
        return this.title;
    }

    public String getDescription(){
        return this.description;
    }

    public Platform getPlatform(){
        return this.platform;
    }

    public WorkItem updateTitle(String title){
        title = (title == null ? null : title.trim());
        if(isPropertyLoaded(TITLE_PROPERTY) && Objects.equals(this.title, title)){
            return this;
        }
        handleUpdate(TITLE_PROPERTY, getTitle(), title);
        this.title = title;
        return this;
    }

    public WorkItem updateDescription(String description){
        description = (description == null ? null : description.trim());
        if(isPropertyLoaded(DESCRIPTION_PROPERTY) && Objects.equals(this.description, description)){
            return this;
        }
        handleUpdate(DESCRIPTION_PROPERTY, getDescription(), description);
        this.description = description;
        return this;
    }

    public WorkItem updatePlatform(Platform platform){
        if(isPropertyLoaded(PLATFORM_PROPERTY) && Objects.equals(this.platform, platform)){
            return this;
        }
        handleUpdate(PLATFORM_PROPERTY, getPlatform(), platform);
        this.platform = platform;
        return this;
    }


    public static WorkItem refer(Long id){
        WorkItem refer = new WorkItem();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public WorkItem comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<WorkItem> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "title": this.title = (value == null ? null : ((String)value).trim()); break;

            case "description": this.description = (value == null ? null : ((String)value).trim()); break;

            case "platform": this.platform = (Platform) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "title": return this.title;
            case "description": return this.description;
            case "platform": return this.platform;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "title": return this.title;
                    case "description": return this.description;
                    case "platform": return this.platform;
                    default: return super.__internalGet(property);
                }
        }
    }

}