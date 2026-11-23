
package com.teaql.merchantorderprivacyservice.customerorder;

import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile;
import com.teaql.merchantorderprivacyservice.merchant.Merchant;
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
public class CustomerOrder extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "CustomerOrder";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "1e5c269646b9d3a77d2b8da3dfb1cf2899c91e276510b91bba15700d02c0d1a3";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("merchant", 1), java.util.Map.entry("customer_profile", 2), java.util.Map.entry("order_number", 3), java.util.Map.entry("fulfillment_status", 4), java.util.Map.entry("version", 5));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("merchant", java.util.List.of("merchant", "merchant")), java.util.Map.entry("customer_profile", java.util.List.of("customerProfile", "customer_profile")), java.util.Map.entry("order_number", java.util.List.of("orderNumber", "order_number")), java.util.Map.entry("fulfillment_status", java.util.List.of("fulfillmentStatus", "fulfillment_status")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            CustomerOrder.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String MERCHANT_PROPERTY = "merchant";
    public static final String CUSTOMER_PROFILE_PROPERTY = "customer_profile";
    public static final String ORDER_NUMBER_PROPERTY = "order_number";
    public static final String FULFILLMENT_STATUS_PROPERTY = "fulfillment_status";
    private Merchant merchant;

    private CustomerProfile customerProfile;

    private String orderNumber;

    private String fulfillmentStatus;


    public Merchant getMerchant(){
        return this.merchant;
    }

    public CustomerProfile getCustomerProfile(){
        return this.customerProfile;
    }

    public String getOrderNumber(){
        return this.orderNumber;
    }

    public String getFulfillmentStatus(){
        return this.fulfillmentStatus;
    }

    public CustomerOrder updateMerchant(Merchant merchant){
        if(isPropertyLoaded(MERCHANT_PROPERTY) && Objects.equals(this.merchant, merchant)){
            return this;
        }
        handleUpdate(MERCHANT_PROPERTY, getMerchant(), merchant);
        this.merchant = merchant;
        return this;
    }

    public CustomerOrder updateCustomerProfile(CustomerProfile customerProfile){
        if(isPropertyLoaded(CUSTOMER_PROFILE_PROPERTY) && Objects.equals(this.customerProfile, customerProfile)){
            return this;
        }
        handleUpdate(CUSTOMER_PROFILE_PROPERTY, getCustomerProfile(), customerProfile);
        this.customerProfile = customerProfile;
        return this;
    }

    public CustomerOrder updateOrderNumber(String orderNumber){
        orderNumber = (orderNumber == null ? null : orderNumber.trim());
        if(isPropertyLoaded(ORDER_NUMBER_PROPERTY) && Objects.equals(this.orderNumber, orderNumber)){
            return this;
        }
        handleUpdate(ORDER_NUMBER_PROPERTY, getOrderNumber(), orderNumber);
        this.orderNumber = orderNumber;
        return this;
    }

    public CustomerOrder updateFulfillmentStatus(String fulfillmentStatus){
        fulfillmentStatus = (fulfillmentStatus == null ? null : fulfillmentStatus.trim());
        if(isPropertyLoaded(FULFILLMENT_STATUS_PROPERTY) && Objects.equals(this.fulfillmentStatus, fulfillmentStatus)){
            return this;
        }
        handleUpdate(FULFILLMENT_STATUS_PROPERTY, getFulfillmentStatus(), fulfillmentStatus);
        this.fulfillmentStatus = fulfillmentStatus;
        return this;
    }


    public static CustomerOrder refer(Long id){
        CustomerOrder refer = new CustomerOrder();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public CustomerOrder comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<CustomerOrder> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "merchant": this.merchant = (Merchant) value; break;

            case "customer_profile": this.customerProfile = (CustomerProfile) value; break;

            case "order_number": this.orderNumber = (value == null ? null : ((String)value).trim()); break;

            case "fulfillment_status": this.fulfillmentStatus = (value == null ? null : ((String)value).trim()); break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "merchant": return this.merchant;
            case "customer_profile": return this.customerProfile;
            case "order_number": return this.orderNumber;
            case "fulfillment_status": return this.fulfillmentStatus;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "merchant": return this.merchant;
                    case "customerProfile": return this.customerProfile;
                    case "orderNumber": return this.orderNumber;
                    case "fulfillmentStatus": return this.fulfillmentStatus;
                    default: return super.__internalGet(property);
                }
        }
    }

}