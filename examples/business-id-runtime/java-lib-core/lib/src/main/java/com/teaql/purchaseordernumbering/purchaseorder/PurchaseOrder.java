
package com.teaql.purchaseordernumbering.purchaseorder;

import com.teaql.purchaseordernumbering.platform.Platform;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.businessid.BusinessIdDefinition;
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
public class PurchaseOrder extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "PurchaseOrder";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "5270760d55c941f82fc30cf0457d609378a6398d6229c0bb5ba44c7dea546787";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("platform", 1), java.util.Map.entry("supplier", 2), java.util.Map.entry("order_number", 3), java.util.Map.entry("version", 4));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("platform", java.util.List.of("platform", "platform")), java.util.Map.entry("supplier", java.util.List.of("supplier", "supplier")), java.util.Map.entry("order_number", java.util.List.of("orderNumber", "order_number")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            PurchaseOrder.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));

    public record OrderNumber(String value) {
        public OrderNumber {
            value = value == null ? null : value.trim();
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("order_number must not be blank");
            }
        }

        public static OrderNumber of(String value) {
            return new OrderNumber(value);
        }

        public static BusinessIdDefinition definition() {
            return new BusinessIdDefinition(
                    "order_number", "daily-permuted-v1", "ORD",
                    "yyyyMMdd", "daily", 6,
                    "-", "order_number", 1);
        }

        @Override
        public String toString() { return value; }
    }

    public static final String PLATFORM_PROPERTY = "platform";
    public static final String SUPPLIER_PROPERTY = "supplier";
    public static final String ORDER_NUMBER_PROPERTY = "order_number";
    private Platform platform;

    private String supplier;

    private String orderNumber;

    public Platform getPlatform(){
        return this.platform;
    }

    public String getSupplier(){
        return this.supplier;
    }

    public OrderNumber getOrderNumber(){
        return this.orderNumber == null ? null : new OrderNumber(this.orderNumber);
    }
    public PurchaseOrder updatePlatform(Platform platform){
        if(isPropertyLoaded(PLATFORM_PROPERTY) && Objects.equals(this.platform, platform)){
            return this;
        }
        handleUpdate(PLATFORM_PROPERTY, getPlatform(), platform);
        this.platform = platform;
        return this;
    }

    public PurchaseOrder updateSupplier(String supplier){
        supplier = (supplier == null ? null : supplier.trim());
        if(isPropertyLoaded(SUPPLIER_PROPERTY) && Objects.equals(this.supplier, supplier)){
            return this;
        }
        handleUpdate(SUPPLIER_PROPERTY, getSupplier(), supplier);
        this.supplier = supplier;
        return this;
    }

    public PurchaseOrder updateOrderNumber(OrderNumber orderNumber){
        String canonical = orderNumber == null ? null : orderNumber.value();
        if(isPropertyLoaded(ORDER_NUMBER_PROPERTY) && Objects.equals(this.orderNumber, canonical)){
            return this;
        }
        handleUpdate(ORDER_NUMBER_PROPERTY, this.orderNumber, canonical);
        this.orderNumber = canonical;
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
            case "platform": this.platform = (Platform) value; break;

            case "supplier": this.supplier = (value == null ? null : ((String)value).trim()); break;

            case "order_number": this.orderNumber = (value == null ? null : ((String)value).trim()); break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "platform": return this.platform;
            case "supplier": return this.supplier;
            case "order_number": return this.orderNumber;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "platform": return this.platform;
                    case "supplier": return this.supplier;
                    case "orderNumber": return this.orderNumber;
                    default: return super.__internalGet(property);
                }
        }
    }

}