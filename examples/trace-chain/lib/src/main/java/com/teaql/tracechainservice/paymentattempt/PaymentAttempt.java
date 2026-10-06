
package com.teaql.tracechainservice.paymentattempt;

import com.teaql.tracechainservice.payment.Payment;
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
public class PaymentAttempt extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "PaymentAttempt";


    public static final String PAYMENT_PROPERTY = "payment";
    public static final String REFERENCE_CODE_PROPERTY = "referenceCode";
    private Payment payment;

    private String referenceCode;


    public Payment getPayment(){
        return this.payment;
    }

    public String getReferenceCode(){
        return this.referenceCode;
    }

    public PaymentAttempt updatePayment(Payment payment){
        if(Objects.equals(this.payment, payment)){
            return this;
        }
        handleUpdate(PAYMENT_PROPERTY, getPayment(), payment);
        this.payment = payment;
        return this;
    }

    public PaymentAttempt updateReferenceCode(String referenceCode){
        referenceCode = (referenceCode == null ? null : referenceCode.trim());
        if(Objects.equals(this.referenceCode, referenceCode)){
            return this;
        }
        handleUpdate(REFERENCE_CODE_PROPERTY, getReferenceCode(), referenceCode);
        this.referenceCode = referenceCode;
        return this;
    }


    public static PaymentAttempt refer(Long id){
        PaymentAttempt refer = new PaymentAttempt();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public PaymentAttempt comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<PaymentAttempt> auditAs(String action) {
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "payment": this.payment = (Payment) value; break;

            case "referenceCode": this.referenceCode = (value == null ? null : ((String)value).trim()); break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "payment": return this.payment;
            case "referenceCode": return this.referenceCode;
            default: return super.__internalGet(property);
        }
    }

}