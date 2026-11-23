
package com.teaql.ordermanagementservice.ordersearchpreset;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
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
public class OrderSearchPreset extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "OrderSearchPreset";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "7370ba923091e78805facef9877cc6c566871604f4974bb9d2fb39be83dcdf83";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("name", 1), java.util.Map.entry("filter_json", 2), java.util.Map.entry("request_id", 3), java.util.Map.entry("owner_user_id", 4), java.util.Map.entry("commerce_platform", 5), java.util.Map.entry("create_time", 6), java.util.Map.entry("update_time", 7), java.util.Map.entry("version", 8));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("filter_json", java.util.List.of("filterJson", "filter_json")), java.util.Map.entry("request_id", java.util.List.of("requestId", "request_id")), java.util.Map.entry("owner_user_id", java.util.List.of("ownerUserId", "owner_user_id")), java.util.Map.entry("commerce_platform", java.util.List.of("commercePlatform", "commerce_platform")), java.util.Map.entry("create_time", java.util.List.of("createTime", "create_time")), java.util.Map.entry("update_time", java.util.List.of("updateTime", "update_time")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            OrderSearchPreset.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String NAME_PROPERTY = "name";
    public static final String FILTER_JSON_PROPERTY = "filter_json";
    public static final String REQUEST_ID_PROPERTY = "request_id";
    public static final String OWNER_USER_ID_PROPERTY = "owner_user_id";
    public static final String COMMERCE_PLATFORM_PROPERTY = "commerce_platform";
    public static final String CREATE_TIME_PROPERTY = "create_time";
    public static final String UPDATE_TIME_PROPERTY = "update_time";
    private String name;

    private String filterJson;

    private String requestId;

    private String ownerUserId;

    private CommercePlatform commercePlatform;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


    public String getName(){
        return this.name;
    }

    public String getFilterJson(){
        return this.filterJson;
    }

    public String getRequestId(){
        return this.requestId;
    }

    public String getOwnerUserId(){
        return this.ownerUserId;
    }

    public CommercePlatform getCommercePlatform(){
        return this.commercePlatform;
    }

    public LocalDateTime getCreateTime(){
        return this.createTime;
    }

    public LocalDateTime getUpdateTime(){
        return this.updateTime;
    }

    public OrderSearchPreset updateName(String name){
        name = (name == null ? null : name.trim());
        if(isPropertyLoaded(NAME_PROPERTY) && Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public OrderSearchPreset updateFilterJson(String filterJson){
        filterJson = (filterJson == null ? null : filterJson.trim());
        if(isPropertyLoaded(FILTER_JSON_PROPERTY) && Objects.equals(this.filterJson, filterJson)){
            return this;
        }
        handleUpdate(FILTER_JSON_PROPERTY, getFilterJson(), filterJson);
        this.filterJson = filterJson;
        return this;
    }

    public OrderSearchPreset updateRequestId(String requestId){
        requestId = (requestId == null ? null : requestId.trim());
        if(isPropertyLoaded(REQUEST_ID_PROPERTY) && Objects.equals(this.requestId, requestId)){
            return this;
        }
        handleUpdate(REQUEST_ID_PROPERTY, getRequestId(), requestId);
        this.requestId = requestId;
        return this;
    }

    public OrderSearchPreset updateOwnerUserId(String ownerUserId){
        ownerUserId = (ownerUserId == null ? null : ownerUserId.trim());
        if(isPropertyLoaded(OWNER_USER_ID_PROPERTY) && Objects.equals(this.ownerUserId, ownerUserId)){
            return this;
        }
        handleUpdate(OWNER_USER_ID_PROPERTY, getOwnerUserId(), ownerUserId);
        this.ownerUserId = ownerUserId;
        return this;
    }

    public OrderSearchPreset updateCommercePlatform(CommercePlatform commercePlatform){
        if(isPropertyLoaded(COMMERCE_PLATFORM_PROPERTY) && Objects.equals(this.commercePlatform, commercePlatform)){
            return this;
        }
        handleUpdate(COMMERCE_PLATFORM_PROPERTY, getCommercePlatform(), commercePlatform);
        this.commercePlatform = commercePlatform;
        return this;
    }

    public OrderSearchPreset updateCreateTime(LocalDateTime createTime){
        if(isPropertyLoaded(CREATE_TIME_PROPERTY) && Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }

    public OrderSearchPreset updateUpdateTime(LocalDateTime updateTime){
        if(isPropertyLoaded(UPDATE_TIME_PROPERTY) && Objects.equals(this.updateTime, updateTime)){
            return this;
        }
        handleUpdate(UPDATE_TIME_PROPERTY, getUpdateTime(), updateTime);
        this.updateTime = updateTime;
        return this;
    }


    public static OrderSearchPreset refer(Long id){
        OrderSearchPreset refer = new OrderSearchPreset();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public OrderSearchPreset comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<OrderSearchPreset> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "filter_json": this.filterJson = (value == null ? null : ((String)value).trim()); break;

            case "request_id": this.requestId = (value == null ? null : ((String)value).trim()); break;

            case "owner_user_id": this.ownerUserId = (value == null ? null : ((String)value).trim()); break;

            case "commerce_platform": this.commercePlatform = (CommercePlatform) value; break;

            case "create_time": this.createTime = (LocalDateTime) value; break;

            case "update_time": this.updateTime = (LocalDateTime) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "filter_json": return this.filterJson;
            case "request_id": return this.requestId;
            case "owner_user_id": return this.ownerUserId;
            case "commerce_platform": return this.commercePlatform;
            case "create_time": return this.createTime;
            case "update_time": return this.updateTime;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "name": return this.name;
                    case "filterJson": return this.filterJson;
                    case "requestId": return this.requestId;
                    case "ownerUserId": return this.ownerUserId;
                    case "commercePlatform": return this.commercePlatform;
                    case "createTime": return this.createTime;
                    case "updateTime": return this.updateTime;
                    default: return super.__internalGet(property);
                }
        }
    }

}