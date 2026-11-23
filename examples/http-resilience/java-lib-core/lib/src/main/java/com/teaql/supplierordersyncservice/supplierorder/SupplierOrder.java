
package com.teaql.supplierordersyncservice.supplierorder;

import com.teaql.supplierordersyncservice.supplier.Supplier;
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
public class SupplierOrder extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "SupplierOrder";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "c0177bc460393fa876fee1a230cdfe3c9269b316b16a102e12484e7b39e1a67f";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("supplier", 1), java.util.Map.entry("external_reference", 2), java.util.Map.entry("sync_status", 3), java.util.Map.entry("version", 4));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("supplier", java.util.List.of("supplier", "supplier")), java.util.Map.entry("external_reference", java.util.List.of("externalReference", "external_reference")), java.util.Map.entry("sync_status", java.util.List.of("syncStatus", "sync_status")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            SupplierOrder.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String SUPPLIER_PROPERTY = "supplier";
    public static final String EXTERNAL_REFERENCE_PROPERTY = "external_reference";
    public static final String SYNC_STATUS_PROPERTY = "sync_status";
    private Supplier supplier;

    private String externalReference;

    private String syncStatus;


    public Supplier getSupplier(){
        return this.supplier;
    }

    public String getExternalReference(){
        return this.externalReference;
    }

    public String getSyncStatus(){
        return this.syncStatus;
    }

    public SupplierOrder updateSupplier(Supplier supplier){
        if(isPropertyLoaded(SUPPLIER_PROPERTY) && Objects.equals(this.supplier, supplier)){
            return this;
        }
        handleUpdate(SUPPLIER_PROPERTY, getSupplier(), supplier);
        this.supplier = supplier;
        return this;
    }

    public SupplierOrder updateExternalReference(String externalReference){
        externalReference = (externalReference == null ? null : externalReference.trim());
        if(isPropertyLoaded(EXTERNAL_REFERENCE_PROPERTY) && Objects.equals(this.externalReference, externalReference)){
            return this;
        }
        handleUpdate(EXTERNAL_REFERENCE_PROPERTY, getExternalReference(), externalReference);
        this.externalReference = externalReference;
        return this;
    }

    public SupplierOrder updateSyncStatus(String syncStatus){
        syncStatus = (syncStatus == null ? null : syncStatus.trim());
        if(isPropertyLoaded(SYNC_STATUS_PROPERTY) && Objects.equals(this.syncStatus, syncStatus)){
            return this;
        }
        handleUpdate(SYNC_STATUS_PROPERTY, getSyncStatus(), syncStatus);
        this.syncStatus = syncStatus;
        return this;
    }


    public static SupplierOrder refer(Long id){
        SupplierOrder refer = new SupplierOrder();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public SupplierOrder comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<SupplierOrder> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "supplier": this.supplier = (Supplier) value; break;

            case "external_reference": this.externalReference = (value == null ? null : ((String)value).trim()); break;

            case "sync_status": this.syncStatus = (value == null ? null : ((String)value).trim()); break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "supplier": return this.supplier;
            case "external_reference": return this.externalReference;
            case "sync_status": return this.syncStatus;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "supplier": return this.supplier;
                    case "externalReference": return this.externalReference;
                    case "syncStatus": return this.syncStatus;
                    default: return super.__internalGet(property);
                }
        }
    }

}