
package com.teaql.ordermanagementservice.ordersearchpreset;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
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

    public static final String NAME_PROPERTY = "name";
    public static final String FILTER_JSON_PROPERTY = "filterJson";
    public static final String REQUEST_ID_PROPERTY = "requestId";
    public static final String OWNER_USER_ID_PROPERTY = "ownerUserId";
    public static final String COMMERCE_PLATFORM_PROPERTY = "commercePlatform";
    public static final String CREATE_TIME_PROPERTY = "createTime";
    public static final String UPDATE_TIME_PROPERTY = "updateTime";
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
        if(Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }
    public OrderSearchPreset updateFilterJson(String filterJson){
        filterJson = (filterJson == null ? null : filterJson.trim());
        if(Objects.equals(this.filterJson, filterJson)){
            return this;
        }
        handleUpdate(FILTER_JSON_PROPERTY, getFilterJson(), filterJson);
        this.filterJson = filterJson;
        return this;
    }
    public OrderSearchPreset updateRequestId(String requestId){
        requestId = (requestId == null ? null : requestId.trim());
        if(Objects.equals(this.requestId, requestId)){
            return this;
        }
        handleUpdate(REQUEST_ID_PROPERTY, getRequestId(), requestId);
        this.requestId = requestId;
        return this;
    }
    public OrderSearchPreset updateOwnerUserId(String ownerUserId){
        ownerUserId = (ownerUserId == null ? null : ownerUserId.trim());
        if(Objects.equals(this.ownerUserId, ownerUserId)){
            return this;
        }
        handleUpdate(OWNER_USER_ID_PROPERTY, getOwnerUserId(), ownerUserId);
        this.ownerUserId = ownerUserId;
        return this;
    }
    public OrderSearchPreset updateCommercePlatform(CommercePlatform commercePlatform){
        if(Objects.equals(this.commercePlatform, commercePlatform)){
            return this;
        }
        handleUpdate(COMMERCE_PLATFORM_PROPERTY, getCommercePlatform(), commercePlatform);
        this.commercePlatform = commercePlatform;
        return this;
    }
    public OrderSearchPreset updateCreateTime(LocalDateTime createTime){
        if(Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }
    public OrderSearchPreset updateUpdateTime(LocalDateTime updateTime){
        if(Objects.equals(this.updateTime, updateTime)){
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
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "filterJson": this.filterJson = (value == null ? null : ((String)value).trim()); break;

            case "requestId": this.requestId = (value == null ? null : ((String)value).trim()); break;

            case "ownerUserId": this.ownerUserId = (value == null ? null : ((String)value).trim()); break;

            case "commercePlatform": this.commercePlatform = (CommercePlatform) value; break;

            case "createTime": this.createTime = (LocalDateTime) value; break;

            case "updateTime": this.updateTime = (LocalDateTime) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
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