
package com.teaql.tracechainservice.orderitem;

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
public class OrderItem extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "OrderItem";


    public static final String CUSTOMER_ORDER_PROPERTY = "customerOrder";
    public static final String NAME_PROPERTY = "name";
    private CustomerOrder customerOrder;

    private String name;


    public CustomerOrder getCustomerOrder(){
        return this.customerOrder;
    }

    public String getName(){
        return this.name;
    }

    public OrderItem updateCustomerOrder(CustomerOrder customerOrder){
        if(Objects.equals(this.customerOrder, customerOrder)){
            return this;
        }
        handleUpdate(CUSTOMER_ORDER_PROPERTY, getCustomerOrder(), customerOrder);
        this.customerOrder = customerOrder;
        return this;
    }

    public OrderItem updateName(String name){
        name = (name == null ? null : name.trim());
        if(Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }


    public static OrderItem refer(Long id){
        OrderItem refer = new OrderItem();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public OrderItem comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<OrderItem> auditAs(String action) {
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "customerOrder": this.customerOrder = (CustomerOrder) value; break;

            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "customerOrder": return this.customerOrder;
            case "name": return this.name;
            default: return super.__internalGet(property);
        }
    }

}