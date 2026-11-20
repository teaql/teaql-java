
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
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
import io.teaql.core.checker.JsonFieldNamingProfile;
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

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "67294964fa1d025f37e472895a7b4548e18b1d34e91dd854877dbc11d0367f28";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("name", 1), java.util.Map.entry("create_time", 2), java.util.Map.entry("update_time", 3), java.util.Map.entry("version", 4));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("create_time", java.util.List.of("createTime", "create_time")), java.util.Map.entry("update_time", java.util.List.of("updateTime", "update_time")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "customer_list", "order_status_list", "customer_order_list", "product_list", "order_line_list", "order_search_preset_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            CommercePlatform.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String NAME_PROPERTY = "name";
    public static final String CREATE_TIME_PROPERTY = "create_time";
    public static final String UPDATE_TIME_PROPERTY = "update_time";
    public static final String CUSTOMER_LIST_PROPERTY = "customer_list";
    public static final String ORDER_STATUS_LIST_PROPERTY = "order_status_list";
    public static final String CUSTOMER_ORDER_LIST_PROPERTY = "customer_order_list";
    public static final String PRODUCT_LIST_PROPERTY = "product_list";
    public static final String ORDER_LINE_LIST_PROPERTY = "order_line_list";
    public static final String ORDER_SEARCH_PRESET_LIST_PROPERTY = "order_search_preset_list";
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
        return this.customerList == null ? SmartList.unloaded() : this.customerList;
    }
    public SmartList<OrderStatus> getOrderStatusList(){
        return this.orderStatusList == null ? SmartList.unloaded() : this.orderStatusList;
    }
    public SmartList<CustomerOrder> getCustomerOrderList(){
        return this.customerOrderList == null ? SmartList.unloaded() : this.customerOrderList;
    }
    public SmartList<Product> getProductList(){
        return this.productList == null ? SmartList.unloaded() : this.productList;
    }
    public SmartList<OrderLine> getOrderLineList(){
        return this.orderLineList == null ? SmartList.unloaded() : this.orderLineList;
    }
    public SmartList<OrderSearchPreset> getOrderSearchPresetList(){
        return this.orderSearchPresetList == null ? SmartList.unloaded() : this.orderSearchPresetList;
    }
    public CommercePlatform updateName(String name){
        name = (name == null ? null : name.trim());
        if(isPropertyLoaded(NAME_PROPERTY) && Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public CommercePlatform updateCreateTime(LocalDateTime createTime){
        if(isPropertyLoaded(CREATE_TIME_PROPERTY) && Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }

    public CommercePlatform updateUpdateTime(LocalDateTime updateTime){
        if(isPropertyLoaded(UPDATE_TIME_PROPERTY) && Objects.equals(this.updateTime, updateTime)){
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
            SmartList<Customer> teaqlUnloadedItems = SmartList.unloaded();
            this.customerList = teaqlUnloadedItems.map(value -> value);
        }

        this.customerList.add(customer);
        customer.updateCommercePlatform(this);
        return this;
    }
    public CommercePlatform addOrderStatus(OrderStatus orderStatus){
        if (orderStatus == null){
            return this;
        }

        if(null == this.orderStatusList){
            SmartList<OrderStatus> teaqlUnloadedItems = SmartList.unloaded();
            this.orderStatusList = teaqlUnloadedItems.map(value -> value);
        }

        this.orderStatusList.add(orderStatus);
        orderStatus.updateCommercePlatform(this);
        return this;
    }
    public CommercePlatform addCustomerOrder(CustomerOrder customerOrder){
        if (customerOrder == null){
            return this;
        }

        if(null == this.customerOrderList){
            SmartList<CustomerOrder> teaqlUnloadedItems = SmartList.unloaded();
            this.customerOrderList = teaqlUnloadedItems.map(value -> value);
        }

        this.customerOrderList.add(customerOrder);
        customerOrder.updateCommercePlatform(this);
        return this;
    }
    public CommercePlatform addProduct(Product product){
        if (product == null){
            return this;
        }

        if(null == this.productList){
            SmartList<Product> teaqlUnloadedItems = SmartList.unloaded();
            this.productList = teaqlUnloadedItems.map(value -> value);
        }

        this.productList.add(product);
        product.updateCommercePlatform(this);
        return this;
    }
    public CommercePlatform addOrderLine(OrderLine orderLine){
        if (orderLine == null){
            return this;
        }

        if(null == this.orderLineList){
            SmartList<OrderLine> teaqlUnloadedItems = SmartList.unloaded();
            this.orderLineList = teaqlUnloadedItems.map(value -> value);
        }

        this.orderLineList.add(orderLine);
        orderLine.updateCommercePlatform(this);
        return this;
    }
    public CommercePlatform addOrderSearchPreset(OrderSearchPreset orderSearchPreset){
        if (orderSearchPreset == null){
            return this;
        }

        if(null == this.orderSearchPresetList){
            SmartList<OrderSearchPreset> teaqlUnloadedItems = SmartList.unloaded();
            this.orderSearchPresetList = teaqlUnloadedItems.map(value -> value);
        }

        this.orderSearchPresetList.add(orderSearchPreset);
        orderSearchPreset.updateCommercePlatform(this);
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
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "create_time": this.createTime = (LocalDateTime) value; break;

            case "update_time": this.updateTime = (LocalDateTime) value; break;

            case "customer_list": this.customerList = (SmartList<Customer>) value; break;
            case "order_status_list": this.orderStatusList = (SmartList<OrderStatus>) value; break;
            case "customer_order_list": this.customerOrderList = (SmartList<CustomerOrder>) value; break;
            case "product_list": this.productList = (SmartList<Product>) value; break;
            case "order_line_list": this.orderLineList = (SmartList<OrderLine>) value; break;
            case "order_search_preset_list": this.orderSearchPresetList = (SmartList<OrderSearchPreset>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "create_time": return this.createTime;
            case "update_time": return this.updateTime;
            case "customer_list": return this.customerList;
            case "order_status_list": return this.orderStatusList;
            case "customer_order_list": return this.customerOrderList;
            case "product_list": return this.productList;
            case "order_line_list": return this.orderLineList;
            case "order_search_preset_list": return this.orderSearchPresetList;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "name": return this.name;
                    case "createTime": return this.createTime;
                    case "updateTime": return this.updateTime;
                    default: return super.__internalGet(property);
                }
        }
    }

}