
package com.teaql.orderattachmentservice.customerorder;

import com.teaql.orderattachmentservice.commerceplatform.CommercePlatform;
import com.teaql.orderattachmentservice.orderattachment.OrderAttachment;
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
public class CustomerOrder extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "CustomerOrder";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "0f43e8a9f92ac574b08050817425a1e7ebc7b9b011cd14c379fdf99b1cc834a0";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("order_number", 1), java.util.Map.entry("commerce_platform", 2), java.util.Map.entry("version", 3));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("order_number", java.util.List.of("orderNumber", "order_number")), java.util.Map.entry("commerce_platform", java.util.List.of("commercePlatform", "commerce_platform")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "order_attachment_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            CustomerOrder.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String ORDER_NUMBER_PROPERTY = "order_number";
    public static final String COMMERCE_PLATFORM_PROPERTY = "commerce_platform";
    public static final String ORDER_ATTACHMENT_LIST_PROPERTY = "order_attachment_list";
    private String orderNumber;

    private CommercePlatform commercePlatform;

    private SmartList<OrderAttachment> orderAttachmentList;

    public String getOrderNumber(){
        return this.orderNumber;
    }

    public CommercePlatform getCommercePlatform(){
        return this.commercePlatform;
    }

    public SmartList<OrderAttachment> getOrderAttachmentList(){
        return this.orderAttachmentList == null ? SmartList.unloaded() : this.orderAttachmentList;
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

    public CustomerOrder updateCommercePlatform(CommercePlatform commercePlatform){
        if(isPropertyLoaded(COMMERCE_PLATFORM_PROPERTY) && Objects.equals(this.commercePlatform, commercePlatform)){
            return this;
        }
        handleUpdate(COMMERCE_PLATFORM_PROPERTY, getCommercePlatform(), commercePlatform);
        this.commercePlatform = commercePlatform;
        return this;
    }

    public CustomerOrder addOrderAttachment(OrderAttachment orderAttachment){
        if (orderAttachment == null){
            return this;
        }

        if(null == this.orderAttachmentList){
            SmartList<OrderAttachment> teaqlUnloadedItems = SmartList.unloaded();
            this.orderAttachmentList = teaqlUnloadedItems.map(value -> value);
        }

        this.orderAttachmentList.add(orderAttachment);
        orderAttachment.updateCustomerOrder(this);
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
            case "order_number": this.orderNumber = (value == null ? null : ((String)value).trim()); break;

            case "commerce_platform": this.commercePlatform = (CommercePlatform) value; break;

            case "order_attachment_list": this.orderAttachmentList = (SmartList<OrderAttachment>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "order_number": return this.orderNumber;
            case "commerce_platform": return this.commercePlatform;
            case "order_attachment_list": return this.orderAttachmentList;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "orderNumber": return this.orderNumber;
                    case "commercePlatform": return this.commercePlatform;
                    default: return super.__internalGet(property);
                }
        }
    }

}