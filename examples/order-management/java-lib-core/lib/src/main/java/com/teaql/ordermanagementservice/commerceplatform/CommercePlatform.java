
package com.teaql.ordermanagementservice.commerceplatform;

import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset;
import com.teaql.ordermanagementservice.orderstatus.OrderStatus;
import com.teaql.ordermanagementservice.product.Product;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * [TEAQL AI WARNING]
 * TeaQL was explicitly designed to PREVENT AI hallucinations and random guessing.
 * DO NOT GUESS METHOD NAMES!
 * The methods listed below are the ONLY valid ways to interact with this entity.
 * If you encounter compilation errors (e.g., method not found), DO NOT guess another method name.
 * Read the method signatures in this file before proceeding.
 */
public class CommercePlatform extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "CommercePlatform";

    public static final String NAME_PROPERTY = "name";
    public static final String CREATE_TIME_PROPERTY = "createTime";
    public static final String UPDATE_TIME_PROPERTY = "updateTime";
    public static final String CUSTOMER_LIST_PROPERTY = "customerList";
    public static final String ORDER_STATUS_LIST_PROPERTY = "orderStatusList";
    public static final String CUSTOMER_ORDER_LIST_PROPERTY = "customerOrderList";
    public static final String PRODUCT_LIST_PROPERTY = "productList";
    public static final String ORDER_LINE_LIST_PROPERTY = "orderLineList";
    public static final String ORDER_SEARCH_PRESET_LIST_PROPERTY = "orderSearchPresetList";
    private String name;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private SmartList<Customer> customerList;
    private SmartList<OrderStatus> orderStatusList;
    private SmartList<CustomerOrder> customerOrderList;
    private SmartList<Product> productList;
    private SmartList<OrderLine> orderLineList;
    private SmartList<OrderSearchPreset> orderSearchPresetList;

    public String getName(){
        return this.name;
    }
    public LocalDateTime getCreateTime(){
        return this.createTime;
    }
    public LocalDateTime getUpdateTime(){
        return this.updateTime;
    }
    public SmartList<Customer> getCustomerList(){
        return this.customerList;
    }
    public SmartList<OrderStatus> getOrderStatusList(){
        return this.orderStatusList;
    }
    public SmartList<CustomerOrder> getCustomerOrderList(){
        return this.customerOrderList;
    }
    public SmartList<Product> getProductList(){
        return this.productList;
    }
    public SmartList<OrderLine> getOrderLineList(){
        return this.orderLineList;
    }
    public SmartList<OrderSearchPreset> getOrderSearchPresetList(){
        return this.orderSearchPresetList;
    }
    public CommercePlatform updateName(String name){
        name = (name == null ? null : name.trim());
        if(Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }
    public CommercePlatform updateCreateTime(LocalDateTime createTime){
        if(Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }
    public CommercePlatform updateUpdateTime(LocalDateTime updateTime){
        if(Objects.equals(this.updateTime, updateTime)){
            return this;
        }
        handleUpdate(UPDATE_TIME_PROPERTY, getUpdateTime(), updateTime);
        this.updateTime = updateTime;
        return this;
    }
    public CommercePlatform addCustomer(Customer customer){
        if (customer == null){
            return this;
        }

        if(null == this.customerList){
            this.customerList = new SmartList<>();
        }

        this.customerList.add(customer);
        customer.cacheRelation(Customer.COMMERCE_PLATFORM_PROPERTY, this);
        return this;
    }
    public CommercePlatform addOrderStatus(OrderStatus orderStatus){
        if (orderStatus == null){
            return this;
        }

        if(null == this.orderStatusList){
            this.orderStatusList = new SmartList<>();
        }

        this.orderStatusList.add(orderStatus);
        orderStatus.cacheRelation(OrderStatus.COMMERCE_PLATFORM_PROPERTY, this);
        return this;
    }
    public CommercePlatform addCustomerOrder(CustomerOrder customerOrder){
        if (customerOrder == null){
            return this;
        }

        if(null == this.customerOrderList){
            this.customerOrderList = new SmartList<>();
        }

        this.customerOrderList.add(customerOrder);
        customerOrder.cacheRelation(CustomerOrder.COMMERCE_PLATFORM_PROPERTY, this);
        return this;
    }
    public CommercePlatform addProduct(Product product){
        if (product == null){
            return this;
        }

        if(null == this.productList){
            this.productList = new SmartList<>();
        }

        this.productList.add(product);
        product.cacheRelation(Product.COMMERCE_PLATFORM_PROPERTY, this);
        return this;
    }
    public CommercePlatform addOrderLine(OrderLine orderLine){
        if (orderLine == null){
            return this;
        }

        if(null == this.orderLineList){
            this.orderLineList = new SmartList<>();
        }

        this.orderLineList.add(orderLine);
        orderLine.cacheRelation(OrderLine.COMMERCE_PLATFORM_PROPERTY, this);
        return this;
    }
    public CommercePlatform addOrderSearchPreset(OrderSearchPreset orderSearchPreset){
        if (orderSearchPreset == null){
            return this;
        }

        if(null == this.orderSearchPresetList){
            this.orderSearchPresetList = new SmartList<>();
        }

        this.orderSearchPresetList.add(orderSearchPreset);
        orderSearchPreset.cacheRelation(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY, this);
        return this;
    }

    public static CommercePlatform refer(Long id){
        CommercePlatform refer = new CommercePlatform();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public CommercePlatform comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<CommercePlatform> auditAs(String action) {
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "createTime": this.createTime = (LocalDateTime) value; break;

            case "updateTime": this.updateTime = (LocalDateTime) value; break;

            case "customerList": this.customerList = (SmartList<Customer>) value; break;
            case "orderStatusList": this.orderStatusList = (SmartList<OrderStatus>) value; break;
            case "customerOrderList": this.customerOrderList = (SmartList<CustomerOrder>) value; break;
            case "productList": this.productList = (SmartList<Product>) value; break;
            case "orderLineList": this.orderLineList = (SmartList<OrderLine>) value; break;
            case "orderSearchPresetList": this.orderSearchPresetList = (SmartList<OrderSearchPreset>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "createTime": return this.createTime;
            case "updateTime": return this.updateTime;
            case "customerList": return this.customerList;
            case "orderStatusList": return this.orderStatusList;
            case "customerOrderList": return this.customerOrderList;
            case "productList": return this.productList;
            case "orderLineList": return this.orderLineList;
            case "orderSearchPresetList": return this.orderSearchPresetList;
            default: return super.__internalGet(property);
        }
    }

}