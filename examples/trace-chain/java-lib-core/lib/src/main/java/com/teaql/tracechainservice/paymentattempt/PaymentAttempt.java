
package com.teaql.tracechainservice.paymentattempt;

import com.teaql.tracechainservice.payment.Payment;
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
public class PaymentAttempt extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "PaymentAttempt";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "e8c46db1ccaafe0f9d90335a74f7dd234482731b27d51dbebb2e9f07bebc02eb";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("payment", 1), java.util.Map.entry("reference_code", 2), java.util.Map.entry("version", 3));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("payment", java.util.List.of("payment", "payment")), java.util.Map.entry("reference_code", java.util.List.of("referenceCode", "reference_code")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            PaymentAttempt.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String PAYMENT_PROPERTY = "payment";
    public static final String REFERENCE_CODE_PROPERTY = "reference_code";
    private Payment payment;

    private String referenceCode;


    public Payment getPayment(){
        return this.payment;
    }

    public String getReferenceCode(){
        return this.referenceCode;
    }

    public PaymentAttempt updatePayment(Payment payment){
        if(isPropertyLoaded(PAYMENT_PROPERTY) && Objects.equals(this.payment, payment)){
            return this;
        }
        handleUpdate(PAYMENT_PROPERTY, getPayment(), payment);
        this.payment = payment;
        return this;
    }

    public PaymentAttempt updateReferenceCode(String referenceCode){
        referenceCode = (referenceCode == null ? null : referenceCode.trim());
        if(isPropertyLoaded(REFERENCE_CODE_PROPERTY) && Objects.equals(this.referenceCode, referenceCode)){
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
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "payment": this.payment = (Payment) value; break;

            case "reference_code": this.referenceCode = (value == null ? null : ((String)value).trim()); break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "payment": return this.payment;
            case "reference_code": return this.referenceCode;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "payment": return this.payment;
                    case "referenceCode": return this.referenceCode;
                    default: return super.__internalGet(property);
                }
        }
    }

}