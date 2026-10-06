
package com.teaql.tracechainservice.shipment;

import com.teaql.tracechainservice.customerorder.CustomerOrder;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import java.util.Objects;

/**
 * [TEAQL AI WARNING]
 * TeaQL was explicitly designed to PREVENT AI hallucinations and random guessing.
 * DO NOT GUESS METHOD NAMES!
 * The methods listed below are the ONLY valid ways to interact with this entity.
 * If you encounter compilation errors (e.g., method not found), DO NOT guess another method name.
 * Read the method signatures in this file before proceeding.
 */
public class Shipment extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "Shipment";


    public static final String CUSTOMER_ORDER_PROPERTY = "customerOrder";
    public static final String REFERENCE_CODE_PROPERTY = "referenceCode";
    private CustomerOrder customerOrder;

    private String referenceCode;


    public CustomerOrder getCustomerOrder(){
        return this.customerOrder;
    }

    public String getReferenceCode(){
        return this.referenceCode;
    }

    public Shipment updateCustomerOrder(CustomerOrder customerOrder){
        if(Objects.equals(this.customerOrder, customerOrder)){
            return this;
        }
        handleUpdate(CUSTOMER_ORDER_PROPERTY, getCustomerOrder(), customerOrder);
        this.customerOrder = customerOrder;
        return this;
    }

    public Shipment updateReferenceCode(String referenceCode){
        referenceCode = (referenceCode == null ? null : referenceCode.trim());
        if(Objects.equals(this.referenceCode, referenceCode)){
            return this;
        }
        handleUpdate(REFERENCE_CODE_PROPERTY, getReferenceCode(), referenceCode);
        this.referenceCode = referenceCode;
        return this;
    }


    public static Shipment refer(Long id){
        Shipment refer = new Shipment();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public Shipment comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<Shipment> auditAs(String action) {
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "customerOrder": this.customerOrder = (CustomerOrder) value; break;

            case "referenceCode": this.referenceCode = (value == null ? null : ((String)value).trim()); break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "customerOrder": return this.customerOrder;
            case "referenceCode": return this.referenceCode;
            default: return super.__internalGet(property);
        }
    }

}