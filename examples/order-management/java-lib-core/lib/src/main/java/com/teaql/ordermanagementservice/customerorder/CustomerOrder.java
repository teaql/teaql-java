
package com.teaql.ordermanagementservice.customerorder;

import com.teaql.ordermanagementservice.Constants;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderstatus.OrderStatus;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
import io.teaql.core.checker.JsonFieldNamingProfile;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class CustomerOrder extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "CustomerOrder";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "e844b19b098f340a5ae18e4091f4a6d55afb35cd5a53ec24155bd4de2601988d";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("order_number", 1), java.util.Map.entry("order_date", 2), java.util.Map.entry("total_amount", 3), java.util.Map.entry("status", 4), java.util.Map.entry("customer", 5), java.util.Map.entry("commerce_platform", 6), java.util.Map.entry("create_time", 7), java.util.Map.entry("update_time", 8), java.util.Map.entry("version", 9));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("order_number", java.util.List.of("orderNumber", "order_number")), java.util.Map.entry("order_date", java.util.List.of("orderDate", "order_date")), java.util.Map.entry("total_amount", java.util.List.of("totalAmount", "total_amount")), java.util.Map.entry("status", java.util.List.of("status", "status")), java.util.Map.entry("customer", java.util.List.of("customer", "customer")), java.util.Map.entry("commerce_platform", java.util.List.of("commercePlatform", "commerce_platform")), java.util.Map.entry("create_time", java.util.List.of("createTime", "create_time")), java.util.Map.entry("update_time", java.util.List.of("updateTime", "update_time")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "order_line_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            CustomerOrder.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String ORDER_NUMBER_PROPERTY = "order_number";
    public static final String ORDER_DATE_PROPERTY = "order_date";
    public static final String TOTAL_AMOUNT_PROPERTY = "total_amount";
    public static final String STATUS_PROPERTY = "status";
    public static final String CUSTOMER_PROPERTY = "customer";
    public static final String COMMERCE_PLATFORM_PROPERTY = "commerce_platform";
    public static final String CREATE_TIME_PROPERTY = "create_time";
    public static final String UPDATE_TIME_PROPERTY = "update_time";
    public static final String ORDER_LINE_LIST_PROPERTY = "order_line_list";
    private String orderNumber;

    private LocalDate orderDate;

    private BigDecimal totalAmount;

    private OrderStatus status;

    private Customer customer;

    private CommercePlatform commercePlatform;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private SmartList<OrderLine> orderLineList;

    public String getOrderNumber(){
        return this.orderNumber;
    }

    public LocalDate getOrderDate(){
        return this.orderDate;
    }

    public BigDecimal getTotalAmount(){
        return this.totalAmount;
    }

    public OrderStatus getStatus(){
        return this.status;
    }

    public Customer getCustomer(){
        return this.customer;
    }

    public CommercePlatform getCommercePlatform(){
        return this.commercePlatform;
    }

    public LocalDateTime getCreateTime(){
        return this.createTime;
    }

    public LocalDateTime getUpdateTime(){
        return this.updateTime;
    }

    public SmartList<OrderLine> getOrderLineList(){
        return this.orderLineList == null ? SmartList.unloaded() : this.orderLineList;
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

    public CustomerOrder updateOrderDate(LocalDate orderDate){
        if(isPropertyLoaded(ORDER_DATE_PROPERTY) && Objects.equals(this.orderDate, orderDate)){
            return this;
        }
        handleUpdate(ORDER_DATE_PROPERTY, getOrderDate(), orderDate);
        this.orderDate = orderDate;
        return this;
    }

    public CustomerOrder updateTotalAmount(BigDecimal totalAmount){
        if(isPropertyLoaded(TOTAL_AMOUNT_PROPERTY) && Objects.equals(this.totalAmount, totalAmount)){
            return this;
        }
        handleUpdate(TOTAL_AMOUNT_PROPERTY, getTotalAmount(), totalAmount);
        this.totalAmount = totalAmount;
        return this;
    }

    protected CustomerOrder updateStatus(OrderStatus status){
        if(isPropertyLoaded(STATUS_PROPERTY) && Objects.equals(this.status, status)){
            return this;
        }
        handleUpdate(STATUS_PROPERTY, getStatus(), status);
        this.status = status;
        return this;
    }

    public CustomerOrder updateCustomer(Customer customer){
        if(isPropertyLoaded(CUSTOMER_PROPERTY) && Objects.equals(this.customer, customer)){
            return this;
        }
        handleUpdate(CUSTOMER_PROPERTY, getCustomer(), customer);
        this.customer = customer;
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

    public CustomerOrder updateCreateTime(LocalDateTime createTime){
        if(isPropertyLoaded(CREATE_TIME_PROPERTY) && Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }

    public CustomerOrder updateUpdateTime(LocalDateTime updateTime){
        if(isPropertyLoaded(UPDATE_TIME_PROPERTY) && Objects.equals(this.updateTime, updateTime)){
            return this;
        }
        handleUpdate(UPDATE_TIME_PROPERTY, getUpdateTime(), updateTime);
        this.updateTime = updateTime;
        return this;
    }

    public CustomerOrder addOrderLine(OrderLine orderLine){
        if (orderLine == null){
            return this;
        }

        if(null == this.orderLineList){
            SmartList<OrderLine> teaqlUnloadedItems = SmartList.unloaded();
            this.orderLineList = teaqlUnloadedItems.map(value -> value);
        }

        this.orderLineList.add(orderLine);
        orderLine.updateCustomerOrder(this);
        return this;
    }
    public boolean isStatusPending(){
        return Objects.equals(getStatus(), Constants.ORDER_STATUS_PENDING);
    }

    public CustomerOrder updateStatusToPending(){
        return updateStatus(Constants.ORDER_STATUS_PENDING);
    }
    public boolean isStatusConfirmed(){
        return Objects.equals(getStatus(), Constants.ORDER_STATUS_CONFIRMED);
    }

    public CustomerOrder updateStatusToConfirmed(){
        return updateStatus(Constants.ORDER_STATUS_CONFIRMED);
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

            case "order_date": this.orderDate = (LocalDate) value; break;

            case "total_amount": this.totalAmount = (BigDecimal) value; break;

            case "status": this.status = (OrderStatus) value; break;

            case "customer": this.customer = (Customer) value; break;

            case "commerce_platform": this.commercePlatform = (CommercePlatform) value; break;

            case "create_time": this.createTime = (LocalDateTime) value; break;

            case "update_time": this.updateTime = (LocalDateTime) value; break;

            case "order_line_list": this.orderLineList = (SmartList<OrderLine>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "order_number": return this.orderNumber;
            case "order_date": return this.orderDate;
            case "total_amount": return this.totalAmount;
            case "status": return this.status;
            case "customer": return this.customer;
            case "commerce_platform": return this.commercePlatform;
            case "create_time": return this.createTime;
            case "update_time": return this.updateTime;
            case "order_line_list": return this.orderLineList;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "orderNumber": return this.orderNumber;
                    case "orderDate": return this.orderDate;
                    case "totalAmount": return this.totalAmount;
                    case "status": return this.status;
                    case "customer": return this.customer;
                    case "commercePlatform": return this.commercePlatform;
                    case "createTime": return this.createTime;
                    case "updateTime": return this.updateTime;
                    default: return super.__internalGet(property);
                }
        }
    }

}