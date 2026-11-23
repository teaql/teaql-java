
package com.example.contextfixservice.entry;

import com.example.contextfixservice.catalog.Catalog;
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
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "7ae3f6f25be2681c105a55532a8c60a599eada522a7c70284e22e2172db13492";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("catalog", 1), java.util.Map.entry("name", 2), java.util.Map.entry("born_at", 3), java.util.Map.entry("touched_at", 4), java.util.Map.entry("source_ip", 5), java.util.Map.entry("last_ip", 6), java.util.Map.entry("opened_by", 7), java.util.Map.entry("changed_by", 8), java.util.Map.entry("guarded_token", 9), java.util.Map.entry("version", 10));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("catalog", java.util.List.of("catalog", "catalog")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("born_at", java.util.List.of("bornAt", "born_at")), java.util.Map.entry("touched_at", java.util.List.of("touchedAt", "touched_at")), java.util.Map.entry("source_ip", java.util.List.of("sourceIp", "source_ip")), java.util.Map.entry("last_ip", java.util.List.of("lastIp", "last_ip")), java.util.Map.entry("opened_by", java.util.List.of("openedBy", "opened_by")), java.util.Map.entry("changed_by", java.util.List.of("changedBy", "changed_by")), java.util.Map.entry("guarded_token", java.util.List.of("guardedToken", "guarded_token")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            Entry.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String CATALOG_PROPERTY = "catalog";
    public static final String NAME_PROPERTY = "name";
    public static final String BORN_AT_PROPERTY = "born_at";
    public static final String TOUCHED_AT_PROPERTY = "touched_at";
    public static final String SOURCE_IP_PROPERTY = "source_ip";
    public static final String LAST_IP_PROPERTY = "last_ip";
    public static final String OPENED_BY_PROPERTY = "opened_by";
    public static final String CHANGED_BY_PROPERTY = "changed_by";
    public static final String GUARDED_TOKEN_PROPERTY = "guarded_token";
    private Catalog catalog;

    private String name;

    private LocalDateTime bornAt;

    private LocalDateTime touchedAt;

    private String sourceIp;

    private String lastIp;

    private String openedBy;

    private String changedBy;

    private String guardedToken;


    public Catalog getCatalog(){
        return this.catalog;
    }

    public String getName(){
        return this.name;
    }

    public LocalDateTime getBornAt(){
        return this.bornAt;
    }

    public LocalDateTime getTouchedAt(){
        return this.touchedAt;
    }

    public String getSourceIp(){
        return this.sourceIp;
    }

    public String getLastIp(){
        return this.lastIp;
    }

    public String getOpenedBy(){
        return this.openedBy;
    }

    public String getChangedBy(){
        return this.changedBy;
    }

    public String getGuardedToken(){
        return this.guardedToken;
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

    public Entry updateBornAt(LocalDateTime bornAt){
        if(isPropertyLoaded(BORN_AT_PROPERTY) && Objects.equals(this.bornAt, bornAt)){
            return this;
        }
        handleUpdate(BORN_AT_PROPERTY, getBornAt(), bornAt);
        this.bornAt = bornAt;
        return this;
    }

    public Entry updateTouchedAt(LocalDateTime touchedAt){
        if(isPropertyLoaded(TOUCHED_AT_PROPERTY) && Objects.equals(this.touchedAt, touchedAt)){
            return this;
        }
        handleUpdate(TOUCHED_AT_PROPERTY, getTouchedAt(), touchedAt);
        this.touchedAt = touchedAt;
        return this;
    }

    public Entry updateSourceIp(String sourceIp){
        sourceIp = (sourceIp == null ? null : sourceIp.trim());
        if(isPropertyLoaded(SOURCE_IP_PROPERTY) && Objects.equals(this.sourceIp, sourceIp)){
            return this;
        }
        handleUpdate(SOURCE_IP_PROPERTY, getSourceIp(), sourceIp);
        this.sourceIp = sourceIp;
        return this;
    }

    public Entry updateLastIp(String lastIp){
        lastIp = (lastIp == null ? null : lastIp.trim());
        if(isPropertyLoaded(LAST_IP_PROPERTY) && Objects.equals(this.lastIp, lastIp)){
            return this;
        }
        handleUpdate(LAST_IP_PROPERTY, getLastIp(), lastIp);
        this.lastIp = lastIp;
        return this;
    }

    public Entry updateOpenedBy(String openedBy){
        openedBy = (openedBy == null ? null : openedBy.trim());
        if(isPropertyLoaded(OPENED_BY_PROPERTY) && Objects.equals(this.openedBy, openedBy)){
            return this;
        }
        handleUpdate(OPENED_BY_PROPERTY, getOpenedBy(), openedBy);
        this.openedBy = openedBy;
        return this;
    }

    public Entry updateChangedBy(String changedBy){
        changedBy = (changedBy == null ? null : changedBy.trim());
        if(isPropertyLoaded(CHANGED_BY_PROPERTY) && Objects.equals(this.changedBy, changedBy)){
            return this;
        }
        handleUpdate(CHANGED_BY_PROPERTY, getChangedBy(), changedBy);
        this.changedBy = changedBy;
        return this;
    }

    public Entry updateGuardedToken(String guardedToken){
        guardedToken = (guardedToken == null ? null : guardedToken.trim());
        if(isPropertyLoaded(GUARDED_TOKEN_PROPERTY) && Objects.equals(this.guardedToken, guardedToken)){
            return this;
        }
        handleUpdate(GUARDED_TOKEN_PROPERTY, getGuardedToken(), guardedToken);
        this.guardedToken = guardedToken;
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

            case "born_at": this.bornAt = (LocalDateTime) value; break;

            case "touched_at": this.touchedAt = (LocalDateTime) value; break;

            case "source_ip": this.sourceIp = (value == null ? null : ((String)value).trim()); break;

            case "last_ip": this.lastIp = (value == null ? null : ((String)value).trim()); break;

            case "opened_by": this.openedBy = (value == null ? null : ((String)value).trim()); break;

            case "changed_by": this.changedBy = (value == null ? null : ((String)value).trim()); break;

            case "guarded_token": this.guardedToken = (value == null ? null : ((String)value).trim()); break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "catalog": return this.catalog;
            case "name": return this.name;
            case "born_at": return this.bornAt;
            case "touched_at": return this.touchedAt;
            case "source_ip": return this.sourceIp;
            case "last_ip": return this.lastIp;
            case "opened_by": return this.openedBy;
            case "changed_by": return this.changedBy;
            case "guarded_token": return this.guardedToken;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "catalog": return this.catalog;
                    case "name": return this.name;
                    case "bornAt": return this.bornAt;
                    case "touchedAt": return this.touchedAt;
                    case "sourceIp": return this.sourceIp;
                    case "lastIp": return this.lastIp;
                    case "openedBy": return this.openedBy;
                    case "changedBy": return this.changedBy;
                    case "guardedToken": return this.guardedToken;
                    default: return super.__internalGet(property);
                }
        }
    }

}