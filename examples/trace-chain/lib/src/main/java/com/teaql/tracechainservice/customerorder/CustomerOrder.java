
package com.teaql.tracechainservice.customerorder;

import com.teaql.tracechainservice.orderitem.OrderItem;
import com.teaql.tracechainservice.payment.Payment;
import com.teaql.tracechainservice.platform.Platform;
import com.teaql.tracechainservice.shipment.Shipment;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
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


    public static final String PLATFORM_PROPERTY = "platform";
    public static final String ORDER_NUMBER_PROPERTY = "orderNumber";
    public static final String DESCRIPTION_PROPERTY = "description";
    public static final String ORDER_ITEM_LIST_PROPERTY = "orderItemList";
    public static final String PAYMENT_LIST_PROPERTY = "paymentList";
    public static final String SHIPMENT_LIST_PROPERTY = "shipmentList";
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
        return this.orderItemList;
    }
    public SmartList<Payment> getPaymentList(){
        return this.paymentList;
    }
    public SmartList<Shipment> getShipmentList(){
        return this.shipmentList;
    }
    public CustomerOrder updatePlatform(Platform platform){
        if(Objects.equals(this.platform, platform)){
            return this;
        }
        handleUpdate(PLATFORM_PROPERTY, getPlatform(), platform);
        this.platform = platform;
        return this;
    }

    public CustomerOrder updateOrderNumber(String orderNumber){
        orderNumber = (orderNumber == null ? null : orderNumber.trim());
        if(Objects.equals(this.orderNumber, orderNumber)){
            return this;
        }
        handleUpdate(ORDER_NUMBER_PROPERTY, getOrderNumber(), orderNumber);
        this.orderNumber = orderNumber;
        return this;
    }

    public CustomerOrder updateDescription(String description){
        description = (description == null ? null : description.trim());
        if(Objects.equals(this.description, description)){
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
            this.orderItemList = new SmartList<>();
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
            this.paymentList = new SmartList<>();
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
            this.shipmentList = new SmartList<>();
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
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "platform": this.platform = (Platform) value; break;

            case "orderNumber": this.orderNumber = (value == null ? null : ((String)value).trim()); break;

            case "description": this.description = (value == null ? null : ((String)value).trim()); break;

            case "orderItemList": this.orderItemList = (SmartList<OrderItem>) value; break;
            case "paymentList": this.paymentList = (SmartList<Payment>) value; break;
            case "shipmentList": this.shipmentList = (SmartList<Shipment>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "platform": return this.platform;
            case "orderNumber": return this.orderNumber;
            case "description": return this.description;
            case "orderItemList": return this.orderItemList;
            case "paymentList": return this.paymentList;
            case "shipmentList": return this.shipmentList;
            default: return super.__internalGet(property);
        }
    }

}