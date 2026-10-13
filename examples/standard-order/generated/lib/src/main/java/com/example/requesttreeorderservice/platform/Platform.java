
package com.example.requesttreeorderservice.platform;

import com.example.requesttreeorderservice.customer.Customer;
import com.example.requesttreeorderservice.orderstatus.OrderStatus;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
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
public class Platform extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "Platform";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "5fc644cb1f2eea3001a51825735535d27d0cefa47fe9e256c92d24309fc93c0f";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("name", 1), java.util.Map.entry("version", 2));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "orderStatusList", "customerList");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            Platform.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES));


    public static final String NAME_PROPERTY = "name";
    public static final String ORDER_STATUS_LIST_PROPERTY = "orderStatusList";
    public static final String CUSTOMER_LIST_PROPERTY = "customerList";
    private String name;

    private SmartList<OrderStatus> orderStatusList;
    private SmartList<Customer> customerList;

    public String getName(){
        return this.name;
    }

    public SmartList<OrderStatus> getOrderStatusList(){
        return this.orderStatusList;
    }
    public SmartList<Customer> getCustomerList(){
        return this.customerList;
    }
    public Platform updateName(String name){
        name = (name == null ? null : name.trim());
        if(Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public Platform addOrderStatus(OrderStatus orderStatus){
        if (orderStatus == null){
            return this;
        }

        if(null == this.orderStatusList){
            this.orderStatusList = new SmartList<>();
        }

        this.orderStatusList.add(orderStatus);
        orderStatus.updatePlatform(this);
        return this;
    }
    public Platform addCustomer(Customer customer){
        if (customer == null){
            return this;
        }

        if(null == this.customerList){
            this.customerList = new SmartList<>();
        }

        this.customerList.add(customer);
        customer.updatePlatform(this);
        return this;
    }

    public static Platform refer(Long id){
        Platform refer = new Platform();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public Platform comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<Platform> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "orderStatusList": this.orderStatusList = (SmartList<OrderStatus>) value; break;
            case "customerList": this.customerList = (SmartList<Customer>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "orderStatusList": return this.orderStatusList;
            case "customerList": return this.customerList;
            default: return super.__internalGet(property);
        }
    }

}