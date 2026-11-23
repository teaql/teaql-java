
package com.example.checkernaturalmessageservice.entry;

import com.example.checkernaturalmessageservice.catalog.Catalog;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
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
public class Entry extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "Entry";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "a24d0c5535a958061d28d3bc95458f9f730a25ed0176dfe41a997281df2a754b";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("catalog", 1), java.util.Map.entry("name", 2), java.util.Map.entry("quantity", 3), java.util.Map.entry("description", 4), java.util.Map.entry("protected_note", 5), java.util.Map.entry("create_time", 6), java.util.Map.entry("update_time", 7), java.util.Map.entry("version", 8));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("catalog", java.util.List.of("catalog", "catalog")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("quantity", java.util.List.of("quantity", "quantity")), java.util.Map.entry("description", java.util.List.of("description", "description")), java.util.Map.entry("protected_note", java.util.List.of("protectedNote", "protected_note")), java.util.Map.entry("create_time", java.util.List.of("createTime", "create_time")), java.util.Map.entry("update_time", java.util.List.of("updateTime", "update_time")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            Entry.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String CATALOG_PROPERTY = "catalog";
    public static final String NAME_PROPERTY = "name";
    public static final String QUANTITY_PROPERTY = "quantity";
    public static final String DESCRIPTION_PROPERTY = "description";
    public static final String PROTECTED_NOTE_PROPERTY = "protected_note";
    public static final String CREATE_TIME_PROPERTY = "create_time";
    public static final String UPDATE_TIME_PROPERTY = "update_time";
    private Catalog catalog;

    private String name;

    private Integer quantity;

    private String description;

    private String protectedNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


    public Catalog getCatalog(){
        return this.catalog;
    }

    public String getName(){
        return this.name;
    }

    public Integer getQuantity(){
        return this.quantity;
    }

    public String getDescription(){
        return this.description;
    }

    public String getProtectedNote(){
        return this.protectedNote;
    }

    public LocalDateTime getCreateTime(){
        return this.createTime;
    }

    public LocalDateTime getUpdateTime(){
        return this.updateTime;
    }

    public Entry updateCatalog(Catalog catalog){
        if(isPropertyLoaded(CATALOG_PROPERTY) && Objects.equals(this.catalog, catalog)){
            return this;
        }
        handleUpdate(CATALOG_PROPERTY, getCatalog(), catalog);
        this.catalog = catalog;
        return this;
    }

    public Entry updateName(String name){
        name = (name == null ? null : name.trim());
        if(isPropertyLoaded(NAME_PROPERTY) && Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public Entry updateQuantity(Integer quantity){
        if(isPropertyLoaded(QUANTITY_PROPERTY) && Objects.equals(this.quantity, quantity)){
            return this;
        }
        handleUpdate(QUANTITY_PROPERTY, getQuantity(), quantity);
        this.quantity = quantity;
        return this;
    }

    public Entry updateDescription(String description){
        description = (description == null ? null : description.trim());
        if(isPropertyLoaded(DESCRIPTION_PROPERTY) && Objects.equals(this.description, description)){
            return this;
        }
        handleUpdate(DESCRIPTION_PROPERTY, getDescription(), description);
        this.description = description;
        return this;
    }

    public Entry updateProtectedNote(String protectedNote){
        protectedNote = (protectedNote == null ? null : protectedNote.trim());
        if(isPropertyLoaded(PROTECTED_NOTE_PROPERTY) && Objects.equals(this.protectedNote, protectedNote)){
            return this;
        }
        handleUpdate(PROTECTED_NOTE_PROPERTY, getProtectedNote(), protectedNote);
        this.protectedNote = protectedNote;
        return this;
    }

    public Entry updateCreateTime(LocalDateTime createTime){
        if(isPropertyLoaded(CREATE_TIME_PROPERTY) && Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }

    public Entry updateUpdateTime(LocalDateTime updateTime){
        if(isPropertyLoaded(UPDATE_TIME_PROPERTY) && Objects.equals(this.updateTime, updateTime)){
            return this;
        }
        handleUpdate(UPDATE_TIME_PROPERTY, getUpdateTime(), updateTime);
        this.updateTime = updateTime;
        return this;
    }


    public static Entry refer(Long id){
        Entry refer = new Entry();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public Entry comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<Entry> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "catalog": this.catalog = (Catalog) value; break;

            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "quantity": this.quantity = (Integer) value; break;

            case "description": this.description = (value == null ? null : ((String)value).trim()); break;

            case "protected_note": this.protectedNote = (value == null ? null : ((String)value).trim()); break;

            case "create_time": this.createTime = (LocalDateTime) value; break;

            case "update_time": this.updateTime = (LocalDateTime) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "catalog": return this.catalog;
            case "name": return this.name;
            case "quantity": return this.quantity;
            case "description": return this.description;
            case "protected_note": return this.protectedNote;
            case "create_time": return this.createTime;
            case "update_time": return this.updateTime;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "catalog": return this.catalog;
                    case "name": return this.name;
                    case "quantity": return this.quantity;
                    case "description": return this.description;
                    case "protectedNote": return this.protectedNote;
                    case "createTime": return this.createTime;
                    case "updateTime": return this.updateTime;
                    default: return super.__internalGet(property);
                }
        }
    }

}