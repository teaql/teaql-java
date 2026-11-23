
package com.teaql.merchantorderprivacyservice.customerprofile;

import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder;
import com.teaql.merchantorderprivacyservice.merchant.Merchant;
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
public class CustomerProfile extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "CustomerProfile";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "5a9cde22ab27c45df77fbacebdb80f8a03759bd10053dc56f76edb4c8b61e319";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("merchant", 1), java.util.Map.entry("email", 2), java.util.Map.entry("version", 3));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("merchant", java.util.List.of("merchant", "merchant")), java.util.Map.entry("email", java.util.List.of("email", "email")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "customer_order_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            CustomerProfile.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String MERCHANT_PROPERTY = "merchant";
    public static final String EMAIL_PROPERTY = "email";
    public static final String CUSTOMER_ORDER_LIST_PROPERTY = "customer_order_list";
    private Merchant merchant;

    private String email;

    private SmartList<CustomerOrder> customerOrderList;

    public Merchant getMerchant(){
        return this.merchant;
    }

    public String getEmail(){
        return this.email;
    }

    public SmartList<CustomerOrder> getCustomerOrderList(){
        return this.customerOrderList == null ? SmartList.unloaded() : this.customerOrderList;
    }
    public CustomerProfile updateMerchant(Merchant merchant){
        if(isPropertyLoaded(MERCHANT_PROPERTY) && Objects.equals(this.merchant, merchant)){
            return this;
        }
        handleUpdate(MERCHANT_PROPERTY, getMerchant(), merchant);
        this.merchant = merchant;
        return this;
    }

    public CustomerProfile updateEmail(String email){
        email = (email == null ? null : email.trim());
        if(isPropertyLoaded(EMAIL_PROPERTY) && Objects.equals(this.email, email)){
            return this;
        }
        handleUpdate(EMAIL_PROPERTY, getEmail(), email);
        this.email = email;
        return this;
    }

    public CustomerProfile addCustomerOrder(CustomerOrder customerOrder){
        if (customerOrder == null){
            return this;
        }

        if(null == this.customerOrderList){
            SmartList<CustomerOrder> teaqlUnloadedItems = SmartList.unloaded();
            this.customerOrderList = teaqlUnloadedItems.map(value -> value);
        }

        this.customerOrderList.add(customerOrder);
        customerOrder.updateCustomerProfile(this);
        return this;
    }

    public static CustomerProfile refer(Long id){
        CustomerProfile refer = new CustomerProfile();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public CustomerProfile comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<CustomerProfile> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "merchant": this.merchant = (Merchant) value; break;

            case "email": this.email = (value == null ? null : ((String)value).trim()); break;

            case "customer_order_list": this.customerOrderList = (SmartList<CustomerOrder>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "merchant": return this.merchant;
            case "email": return this.email;
            case "customer_order_list": return this.customerOrderList;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "merchant": return this.merchant;
                    case "email": return this.email;
                    default: return super.__internalGet(property);
                }
        }
    }

}