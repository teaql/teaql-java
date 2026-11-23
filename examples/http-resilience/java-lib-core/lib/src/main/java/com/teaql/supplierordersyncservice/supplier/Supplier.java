
package com.teaql.supplierordersyncservice.supplier;

import com.teaql.supplierordersyncservice.supplierorder.SupplierOrder;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
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
public class Supplier extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "Supplier";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "235b3035c863f7dc4a320bb8cc07e1eb0f61b36fab66ddcf3dc268357a9d89e8";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("name", 1), java.util.Map.entry("version", 2));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "supplier_order_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            Supplier.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String NAME_PROPERTY = "name";
    public static final String SUPPLIER_ORDER_LIST_PROPERTY = "supplier_order_list";
    private String name;

    private SmartList<SupplierOrder> supplierOrderList;

    public String getName(){
        return this.name;
    }

    public SmartList<SupplierOrder> getSupplierOrderList(){
        return this.supplierOrderList == null ? SmartList.unloaded() : this.supplierOrderList;
    }
    public Supplier updateName(String name){
        name = (name == null ? null : name.trim());
        if(isPropertyLoaded(NAME_PROPERTY) && Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public Supplier addSupplierOrder(SupplierOrder supplierOrder){
        if (supplierOrder == null){
            return this;
        }

        if(null == this.supplierOrderList){
            SmartList<SupplierOrder> teaqlUnloadedItems = SmartList.unloaded();
            this.supplierOrderList = teaqlUnloadedItems.map(value -> value);
        }

        this.supplierOrderList.add(supplierOrder);
        supplierOrder.updateSupplier(this);
        return this;
    }

    public static Supplier refer(Long id){
        Supplier refer = new Supplier();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public Supplier comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<Supplier> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "supplier_order_list": this.supplierOrderList = (SmartList<SupplierOrder>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "supplier_order_list": return this.supplierOrderList;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "name": return this.name;
                    default: return super.__internalGet(property);
                }
        }
    }

}