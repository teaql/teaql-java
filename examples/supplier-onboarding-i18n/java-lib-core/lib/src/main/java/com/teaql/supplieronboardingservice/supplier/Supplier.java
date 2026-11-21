
package com.teaql.supplieronboardingservice.supplier;

import com.teaql.supplieronboardingservice.warehouse.Warehouse;
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
public class Supplier extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "Supplier";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "f097b94d1d63a6466f5d9902e229b6332e48071c02492daa09de0a8717d781db";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("warehouse", 1), java.util.Map.entry("name", 2), java.util.Map.entry("minimum_order_quantity", 3), java.util.Map.entry("version", 4));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("warehouse", java.util.List.of("warehouse", "warehouse")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("minimum_order_quantity", java.util.List.of("minimumOrderQuantity", "minimum_order_quantity")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            Supplier.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String WAREHOUSE_PROPERTY = "warehouse";
    public static final String NAME_PROPERTY = "name";
    public static final String MINIMUM_ORDER_QUANTITY_PROPERTY = "minimum_order_quantity";
    private Warehouse warehouse;

    private String name;

    private Integer minimumOrderQuantity;


    public Warehouse getWarehouse(){
        return this.warehouse;
    }

    public String getName(){
        return this.name;
    }

    public Integer getMinimumOrderQuantity(){
        return this.minimumOrderQuantity;
    }

    public Supplier updateWarehouse(Warehouse warehouse){
        if(isPropertyLoaded(WAREHOUSE_PROPERTY) && Objects.equals(this.warehouse, warehouse)){
            return this;
        }
        handleUpdate(WAREHOUSE_PROPERTY, getWarehouse(), warehouse);
        this.warehouse = warehouse;
        return this;
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

    public Supplier updateMinimumOrderQuantity(Integer minimumOrderQuantity){
        if(isPropertyLoaded(MINIMUM_ORDER_QUANTITY_PROPERTY) && Objects.equals(this.minimumOrderQuantity, minimumOrderQuantity)){
            return this;
        }
        handleUpdate(MINIMUM_ORDER_QUANTITY_PROPERTY, getMinimumOrderQuantity(), minimumOrderQuantity);
        this.minimumOrderQuantity = minimumOrderQuantity;
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
            case "warehouse": this.warehouse = (Warehouse) value; break;

            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "minimum_order_quantity": this.minimumOrderQuantity = (Integer) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "warehouse": return this.warehouse;
            case "name": return this.name;
            case "minimum_order_quantity": return this.minimumOrderQuantity;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "warehouse": return this.warehouse;
                    case "name": return this.name;
                    case "minimumOrderQuantity": return this.minimumOrderQuantity;
                    default: return super.__internalGet(property);
                }
        }
    }

}