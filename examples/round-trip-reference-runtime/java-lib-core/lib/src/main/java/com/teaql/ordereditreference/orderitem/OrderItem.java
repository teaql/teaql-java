
package com.teaql.ordereditreference.orderitem;

import com.teaql.ordereditreference.salesorder.SalesOrder;
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
public class OrderItem extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "OrderItem";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "8d70859137f59ca1b9c6e9daa84dde3e7300bdbbb95ca56c147662302a8a6aba";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("sales_order", 1), java.util.Map.entry("sku", 2), java.util.Map.entry("quantity", 3), java.util.Map.entry("version", 4));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("sales_order", java.util.List.of("salesOrder", "sales_order")), java.util.Map.entry("sku", java.util.List.of("sku", "sku")), java.util.Map.entry("quantity", java.util.List.of("quantity", "quantity")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            OrderItem.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String SALES_ORDER_PROPERTY = "sales_order";
    public static final String SKU_PROPERTY = "sku";
    public static final String QUANTITY_PROPERTY = "quantity";
    private SalesOrder salesOrder;

    private String sku;

    private Integer quantity;


    public SalesOrder getSalesOrder(){
        return this.salesOrder;
    }

    public String getSku(){
        return this.sku;
    }

    public Integer getQuantity(){
        return this.quantity;
    }

    public OrderItem updateSalesOrder(SalesOrder salesOrder){
        if(isPropertyLoaded(SALES_ORDER_PROPERTY) && Objects.equals(this.salesOrder, salesOrder)){
            return this;
        }
        handleUpdate(SALES_ORDER_PROPERTY, getSalesOrder(), salesOrder);
        this.salesOrder = salesOrder;
        return this;
    }

    public OrderItem updateSku(String sku){
        sku = (sku == null ? null : sku.trim());
        if(isPropertyLoaded(SKU_PROPERTY) && Objects.equals(this.sku, sku)){
            return this;
        }
        handleUpdate(SKU_PROPERTY, getSku(), sku);
        this.sku = sku;
        return this;
    }

    public OrderItem updateQuantity(Integer quantity){
        if(isPropertyLoaded(QUANTITY_PROPERTY) && Objects.equals(this.quantity, quantity)){
            return this;
        }
        handleUpdate(QUANTITY_PROPERTY, getQuantity(), quantity);
        this.quantity = quantity;
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
            case "sales_order": this.salesOrder = (SalesOrder) value; break;

            case "sku": this.sku = (value == null ? null : ((String)value).trim()); break;

            case "quantity": this.quantity = (Integer) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "sales_order": return this.salesOrder;
            case "sku": return this.sku;
            case "quantity": return this.quantity;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "salesOrder": return this.salesOrder;
                    case "sku": return this.sku;
                    case "quantity": return this.quantity;
                    default: return super.__internalGet(property);
                }
        }
    }

}