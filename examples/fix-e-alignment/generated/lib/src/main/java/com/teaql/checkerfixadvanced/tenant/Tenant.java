
package com.teaql.checkerfixadvanced.tenant;

import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
import java.util.Objects;

/**
 * [TEAQL AI WARNING]
 * TeaQL was explicitly designed to PREVENT AI hallucinations and random guessing.
 * DO NOT GUESS METHOD NAMES!
 * The methods listed below are the ONLY valid ways to interact with this entity.
 * If you encounter compilation errors (e.g., method not found), DO NOT guess another method name.
 * Read the method signatures in this file before proceeding.
 */
public class Tenant extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "Tenant";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "85c7e7a1c8e2088fabba9ff66843736d36af1abc05a96ca7c72123129cf55183";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("name", 1), java.util.Map.entry("version", 2));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "purchase_order_list", "order_item_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            Tenant.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES));


    public static final String NAME_PROPERTY = "name";
    public static final String PURCHASE_ORDER_LIST_PROPERTY = "purchase_order_list";
    public static final String ORDER_ITEM_LIST_PROPERTY = "order_item_list";
    private String name;

    private SmartList<PurchaseOrder> purchaseOrderList;
    private SmartList<OrderItem> orderItemList;

    public String getName(){
        return this.name;
    }

    public SmartList<PurchaseOrder> getPurchaseOrderList(){
        return this.purchaseOrderList;
    }
    public SmartList<OrderItem> getOrderItemList(){
        return this.orderItemList;
    }
    public Tenant updateName(String name){
        name = (name == null ? null : name.trim());
        if(Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public Tenant addPurchaseOrder(PurchaseOrder purchaseOrder){
        if (purchaseOrder == null){
            return this;
        }

        if(null == this.purchaseOrderList){
            this.purchaseOrderList = new SmartList<>();
        }

        this.purchaseOrderList.add(purchaseOrder);
        purchaseOrder.cacheRelation(PurchaseOrder.TENANT_PROPERTY, this);
        return this;
    }
    public Tenant addOrderItem(OrderItem orderItem){
        if (orderItem == null){
            return this;
        }

        if(null == this.orderItemList){
            this.orderItemList = new SmartList<>();
        }

        this.orderItemList.add(orderItem);
        orderItem.cacheRelation(OrderItem.TENANT_PROPERTY, this);
        return this;
    }

    public static Tenant refer(Long id){
        Tenant refer = new Tenant();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public Tenant comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<Tenant> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "purchase_order_list": this.purchaseOrderList = (SmartList<PurchaseOrder>) value; break;
            case "order_item_list": this.orderItemList = (SmartList<OrderItem>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "purchase_order_list": return this.purchaseOrderList;
            case "order_item_list": return this.orderItemList;
            default: return super.__internalGet(property);
        }
    }

}