
package com.teaql.tracechainservice.payment;

import com.teaql.tracechainservice.customerorder.CustomerOrder;
import com.teaql.tracechainservice.paymentattempt.PaymentAttempt;
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
public class Payment extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "Payment";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "7a1e8a9603ba1bf577f0e3318f5c68e0b1e8d16c08f14ba596bf5292689211cb";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("customer_order", 1), java.util.Map.entry("reference_code", 2), java.util.Map.entry("version", 3));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("customer_order", java.util.List.of("customerOrder", "customer_order")), java.util.Map.entry("reference_code", java.util.List.of("referenceCode", "reference_code")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "payment_attempt_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            Payment.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String CUSTOMER_ORDER_PROPERTY = "customer_order";
    public static final String REFERENCE_CODE_PROPERTY = "reference_code";
    public static final String PAYMENT_ATTEMPT_LIST_PROPERTY = "payment_attempt_list";
    private CustomerOrder customerOrder;

    private String referenceCode;

    private SmartList<PaymentAttempt> paymentAttemptList;

    public CustomerOrder getCustomerOrder(){
        return this.customerOrder;
    }

    public String getReferenceCode(){
        return this.referenceCode;
    }

    public SmartList<PaymentAttempt> getPaymentAttemptList(){
        return this.paymentAttemptList == null ? SmartList.unloaded() : this.paymentAttemptList;
    }
    public Payment updateCustomerOrder(CustomerOrder customerOrder){
        if(isPropertyLoaded(CUSTOMER_ORDER_PROPERTY) && Objects.equals(this.customerOrder, customerOrder)){
            return this;
        }
        handleUpdate(CUSTOMER_ORDER_PROPERTY, getCustomerOrder(), customerOrder);
        this.customerOrder = customerOrder;
        return this;
    }

    public Payment updateReferenceCode(String referenceCode){
        referenceCode = (referenceCode == null ? null : referenceCode.trim());
        if(isPropertyLoaded(REFERENCE_CODE_PROPERTY) && Objects.equals(this.referenceCode, referenceCode)){
            return this;
        }
        handleUpdate(REFERENCE_CODE_PROPERTY, getReferenceCode(), referenceCode);
        this.referenceCode = referenceCode;
        return this;
    }

    public Payment addPaymentAttempt(PaymentAttempt paymentAttempt){
        if (paymentAttempt == null){
            return this;
        }

        if(null == this.paymentAttemptList){
            SmartList<PaymentAttempt> teaqlUnloadedItems = SmartList.unloaded();
            this.paymentAttemptList = teaqlUnloadedItems.map(value -> value);
        }

        this.paymentAttemptList.add(paymentAttempt);
        paymentAttempt.updatePayment(this);
        return this;
    }

    public static Payment refer(Long id){
        Payment refer = new Payment();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public Payment comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<Payment> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "customer_order": this.customerOrder = (CustomerOrder) value; break;

            case "reference_code": this.referenceCode = (value == null ? null : ((String)value).trim()); break;

            case "payment_attempt_list": this.paymentAttemptList = (SmartList<PaymentAttempt>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "customer_order": return this.customerOrder;
            case "reference_code": return this.referenceCode;
            case "payment_attempt_list": return this.paymentAttemptList;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "customerOrder": return this.customerOrder;
                    case "referenceCode": return this.referenceCode;
                    default: return super.__internalGet(property);
                }
        }
    }

}