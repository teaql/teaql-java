
package com.teaql.checkerfixadvanced.orderitem;

import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import com.teaql.checkerfixadvanced.tenant.Tenant;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.checker.JsonFieldNamingProfile;
import java.math.BigDecimal;
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
public class OrderItem extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "OrderItem";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "7d17fd30f99ff7729c89502083418f85d482d6c6b7448972408e96e73ae79d75";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("purchase_order", 1), java.util.Map.entry("tenant", 2), java.util.Map.entry("product_name", 3), java.util.Map.entry("unit_price", 4), java.util.Map.entry("create_time", 5), java.util.Map.entry("update_time", 6), java.util.Map.entry("version", 7));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("purchase_order", java.util.List.of("purchaseOrder", "purchase_order")), java.util.Map.entry("tenant", java.util.List.of("tenant", "tenant")), java.util.Map.entry("product_name", java.util.List.of("productName", "product_name")), java.util.Map.entry("unit_price", java.util.List.of("unitPrice", "unit_price")), java.util.Map.entry("create_time", java.util.List.of("createTime", "create_time")), java.util.Map.entry("update_time", java.util.List.of("updateTime", "update_time")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            OrderItem.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String PURCHASE_ORDER_PROPERTY = "purchase_order";
    public static final String TENANT_PROPERTY = "tenant";
    public static final String PRODUCT_NAME_PROPERTY = "product_name";
    public static final String UNIT_PRICE_PROPERTY = "unit_price";
    public static final String CREATE_TIME_PROPERTY = "create_time";
    public static final String UPDATE_TIME_PROPERTY = "update_time";
    private PurchaseOrder purchaseOrder;

    private Tenant tenant;

    private String productName;

    private BigDecimal unitPrice;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


    public PurchaseOrder getPurchaseOrder(){
        return this.purchaseOrder;
    }

    public Tenant getTenant(){
        return this.tenant;
    }

    public String getProductName(){
        return this.productName;
    }

    public BigDecimal getUnitPrice(){
        return this.unitPrice;
    }

    public LocalDateTime getCreateTime(){
        return this.createTime;
    }

    public LocalDateTime getUpdateTime(){
        return this.updateTime;
    }

    public OrderItem updatePurchaseOrder(PurchaseOrder purchaseOrder){
        if(isPropertyLoaded(PURCHASE_ORDER_PROPERTY) && Objects.equals(this.purchaseOrder, purchaseOrder)){
            return this;
        }
        handleUpdate(PURCHASE_ORDER_PROPERTY, getPurchaseOrder(), purchaseOrder);
        this.purchaseOrder = purchaseOrder;
        return this;
    }

     OrderItem updateTenant(Tenant tenant){
        if(isPropertyLoaded(TENANT_PROPERTY) && Objects.equals(this.tenant, tenant)){
            return this;
        }
        handleUpdate(TENANT_PROPERTY, getTenant(), tenant);
        this.tenant = tenant;
        return this;
    }

    public OrderItem updateProductName(String productName){
        productName = (productName == null ? null : productName.trim());
        if(isPropertyLoaded(PRODUCT_NAME_PROPERTY) && Objects.equals(this.productName, productName)){
            return this;
        }
        handleUpdate(PRODUCT_NAME_PROPERTY, getProductName(), productName);
        this.productName = productName;
        return this;
    }

    public OrderItem updateUnitPrice(BigDecimal unitPrice){
        if(isPropertyLoaded(UNIT_PRICE_PROPERTY) && Objects.equals(this.unitPrice, unitPrice)){
            return this;
        }
        handleUpdate(UNIT_PRICE_PROPERTY, getUnitPrice(), unitPrice);
        this.unitPrice = unitPrice;
        return this;
    }

    public OrderItem updateCreateTime(LocalDateTime createTime){
        if(isPropertyLoaded(CREATE_TIME_PROPERTY) && Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }

    public OrderItem updateUpdateTime(LocalDateTime updateTime){
        if(isPropertyLoaded(UPDATE_TIME_PROPERTY) && Objects.equals(this.updateTime, updateTime)){
            return this;
        }
        handleUpdate(UPDATE_TIME_PROPERTY, getUpdateTime(), updateTime);
        this.updateTime = updateTime;
        return this;
    }


    public static OrderItem refer(Long id){
        OrderItem refer = new OrderItem();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public OrderItem comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<OrderItem> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "purchase_order": this.purchaseOrder = (PurchaseOrder) value; break;

            case "tenant": this.tenant = (Tenant) value; break;

            case "product_name": this.productName = (value == null ? null : ((String)value).trim()); break;

            case "unit_price": this.unitPrice = (BigDecimal) value; break;

            case "create_time": this.createTime = (LocalDateTime) value; break;

            case "update_time": this.updateTime = (LocalDateTime) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "purchase_order": return this.purchaseOrder;
            case "tenant": return this.tenant;
            case "product_name": return this.productName;
            case "unit_price": return this.unitPrice;
            case "create_time": return this.createTime;
            case "update_time": return this.updateTime;
            default: return super.__internalGet(property);
        }
    }

}