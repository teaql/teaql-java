
package com.teaql.checkerfixadvanced.purchaseorder;

import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.tenant.Tenant;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
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
public class PurchaseOrder extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "PurchaseOrder";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "fc779222657a345b62cacc08e4beb1351e6097a5fd25927366703543a4afdd39";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("tenant", 1), java.util.Map.entry("contact_phone", 2), java.util.Map.entry("internal_note", 3), java.util.Map.entry("create_time", 4), java.util.Map.entry("update_time", 5), java.util.Map.entry("version", 6));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("tenant", java.util.List.of("tenant", "tenant")), java.util.Map.entry("contact_phone", java.util.List.of("contactPhone", "contact_phone")), java.util.Map.entry("internal_note", java.util.List.of("internalNote", "internal_note")), java.util.Map.entry("create_time", java.util.List.of("createTime", "create_time")), java.util.Map.entry("update_time", java.util.List.of("updateTime", "update_time")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "order_item_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            PurchaseOrder.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES));


    public static final String TENANT_PROPERTY = "tenant";
    public static final String CONTACT_PHONE_PROPERTY = "contact_phone";
    public static final String INTERNAL_NOTE_PROPERTY = "internal_note";
    public static final String CREATE_TIME_PROPERTY = "create_time";
    public static final String UPDATE_TIME_PROPERTY = "update_time";
    public static final String ORDER_ITEM_LIST_PROPERTY = "order_item_list";
    private Tenant tenant;

    private String contactPhone;

    private String internalNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private SmartList<OrderItem> orderItemList;

    public Tenant getTenant(){
        return this.tenant;
    }

    public String getContactPhone(){
        return this.contactPhone;
    }

    public String getInternalNote(){
        return this.internalNote;
    }

    public LocalDateTime getCreateTime(){
        return this.createTime;
    }

    public LocalDateTime getUpdateTime(){
        return this.updateTime;
    }

    public SmartList<OrderItem> getOrderItemList(){
        return this.orderItemList;
    }
     PurchaseOrder updateTenant(Tenant tenant){
        if(Objects.equals(this.tenant, tenant)){
            return this;
        }
        handleUpdate(TENANT_PROPERTY, getTenant(), tenant);
        this.tenant = tenant;
        return this;
    }

    public PurchaseOrder updateContactPhone(String contactPhone){
        contactPhone = (contactPhone == null ? null : contactPhone.trim());
        if(Objects.equals(this.contactPhone, contactPhone)){
            return this;
        }
        handleUpdate(CONTACT_PHONE_PROPERTY, getContactPhone(), contactPhone);
        this.contactPhone = contactPhone;
        return this;
    }

    public PurchaseOrder updateInternalNote(String internalNote){
        internalNote = (internalNote == null ? null : internalNote.trim());
        if(Objects.equals(this.internalNote, internalNote)){
            return this;
        }
        handleUpdate(INTERNAL_NOTE_PROPERTY, getInternalNote(), internalNote);
        this.internalNote = internalNote;
        return this;
    }

    public PurchaseOrder updateCreateTime(LocalDateTime createTime){
        if(Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }

    public PurchaseOrder updateUpdateTime(LocalDateTime updateTime){
        if(Objects.equals(this.updateTime, updateTime)){
            return this;
        }
        handleUpdate(UPDATE_TIME_PROPERTY, getUpdateTime(), updateTime);
        this.updateTime = updateTime;
        return this;
    }

    public PurchaseOrder addOrderItem(OrderItem orderItem){
        if (orderItem == null){
            return this;
        }

        if(null == this.orderItemList){
            this.orderItemList = new SmartList<>();
        }

        this.orderItemList.add(orderItem);
        orderItem.updatePurchaseOrder(this);
        return this;
    }

    public static PurchaseOrder refer(Long id){
        PurchaseOrder refer = new PurchaseOrder();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public PurchaseOrder comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<PurchaseOrder> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "tenant": this.tenant = (Tenant) value; break;

            case "contact_phone": this.contactPhone = (value == null ? null : ((String)value).trim()); break;

            case "internal_note": this.internalNote = (value == null ? null : ((String)value).trim()); break;

            case "create_time": this.createTime = (LocalDateTime) value; break;

            case "update_time": this.updateTime = (LocalDateTime) value; break;

            case "order_item_list": this.orderItemList = (SmartList<OrderItem>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "tenant": return this.tenant;
            case "contact_phone": return this.contactPhone;
            case "internal_note": return this.internalNote;
            case "create_time": return this.createTime;
            case "update_time": return this.updateTime;
            case "order_item_list": return this.orderItemList;
            default: return super.__internalGet(property);
        }
    }

}