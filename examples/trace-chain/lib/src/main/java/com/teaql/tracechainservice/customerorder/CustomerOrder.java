
package com.teaql.tracechainservice.customerorder;

import com.teaql.tracechainservice.orderitem.OrderItem;
import com.teaql.tracechainservice.payment.Payment;
import com.teaql.tracechainservice.platform.Platform;
import com.teaql.tracechainservice.shipment.Shipment;
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
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "f10d535b66998395625745c0c3f6032ed01350e7f851687673b5ff1eb94a4337";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("platform", 1), java.util.Map.entry("order_number", 2), java.util.Map.entry("description", 3), java.util.Map.entry("version", 4));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("platform", java.util.List.of("platform", "platform")), java.util.Map.entry("order_number", java.util.List.of("orderNumber", "order_number")), java.util.Map.entry("description", java.util.List.of("description", "description")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "order_item_list", "payment_list", "shipment_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            CustomerOrder.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String PLATFORM_PROPERTY = "platform";
    public static final String ORDER_NUMBER_PROPERTY = "order_number";
    public static final String DESCRIPTION_PROPERTY = "description";
    public static final String ORDER_ITEM_LIST_PROPERTY = "order_item_list";
    public static final String PAYMENT_LIST_PROPERTY = "payment_list";
    public static final String SHIPMENT_LIST_PROPERTY = "shipment_list";
    private Platform platform;

    private String orderNumber;

    private String description;

    private SmartList<OrderItem> orderItemList;
    private SmartList<Payment> paymentList;
    private SmartList<Shipment> shipmentList;

    public Platform getPlatform(){
        return this.platform;
    }

    public String getOrderNumber(){
        return this.orderNumber;
    }

    public String getDescription(){
        return this.description;
    }

    public SmartList<OrderItem> getOrderItemList(){
        return this.orderItemList == null ? SmartList.unloaded() : this.orderItemList;
    }
    public SmartList<Payment> getPaymentList(){
        return this.paymentList == null ? SmartList.unloaded() : this.paymentList;
    }
    public SmartList<Shipment> getShipmentList(){
        return this.shipmentList == null ? SmartList.unloaded() : this.shipmentList;
    }
    public CustomerOrder updatePlatform(Platform platform){
        if(isPropertyLoaded(PLATFORM_PROPERTY) && Objects.equals(this.platform, platform)){
            return this;
        }
        handleUpdate(PLATFORM_PROPERTY, getPlatform(), platform);
        this.platform = platform;
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

    public CustomerOrder updateDescription(String description){
        description = (description == null ? null : description.trim());
        if(isPropertyLoaded(DESCRIPTION_PROPERTY) && Objects.equals(this.description, description)){
            return this;
        }
        handleUpdate(DESCRIPTION_PROPERTY, getDescription(), description);
        this.description = description;
        return this;
    }

    public CustomerOrder addOrderItem(OrderItem orderItem){
        if (orderItem == null){
            return this;
        }

        if(null == this.orderItemList){
            SmartList<OrderItem> teaqlUnloadedItems = SmartList.unloaded();
            this.orderItemList = teaqlUnloadedItems.map(value -> value);
        }

        this.orderItemList.add(orderItem);
        orderItem.updateCustomerOrder(this);
        return this;
    }
    public CustomerOrder addPayment(Payment payment){
        if (payment == null){
            return this;
        }

        if(null == this.paymentList){
            SmartList<Payment> teaqlUnloadedItems = SmartList.unloaded();
            this.paymentList = teaqlUnloadedItems.map(value -> value);
        }

        this.paymentList.add(payment);
        payment.updateCustomerOrder(this);
        return this;
    }
    public CustomerOrder addShipment(Shipment shipment){
        if (shipment == null){
            return this;
        }

        if(null == this.shipmentList){
            SmartList<Shipment> teaqlUnloadedItems = SmartList.unloaded();
            this.shipmentList = teaqlUnloadedItems.map(value -> value);
        }

        this.shipmentList.add(shipment);
        shipment.updateCustomerOrder(this);
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
            case "platform": this.platform = (Platform) value; break;

            case "order_number": this.orderNumber = (value == null ? null : ((String)value).trim()); break;

            case "description": this.description = (value == null ? null : ((String)value).trim()); break;

            case "order_item_list": this.orderItemList = (SmartList<OrderItem>) value; break;
            case "payment_list": this.paymentList = (SmartList<Payment>) value; break;
            case "shipment_list": this.shipmentList = (SmartList<Shipment>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "platform": return this.platform;
            case "order_number": return this.orderNumber;
            case "description": return this.description;
            case "order_item_list": return this.orderItemList;
            case "payment_list": return this.paymentList;
            case "shipment_list": return this.shipmentList;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "platform": return this.platform;
                    case "orderNumber": return this.orderNumber;
                    case "description": return this.description;
                    default: return super.__internalGet(property);
                }
        }
    }

}