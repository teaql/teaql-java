
package com.example.requesttreeorderservice.customerorder;

import com.example.requesttreeorderservice.Constants;
import com.example.requesttreeorderservice.customer.Customer;
import com.example.requesttreeorderservice.orderitem.OrderItem;
import com.example.requesttreeorderservice.orderstatus.OrderStatus;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "835b87e26813b0d288c20fbcd3ab52649d0d32277d4f1740fac610ad0b9432bb";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("customer", 1), java.util.Map.entry("status", 2), java.util.Map.entry("order_number", 3), java.util.Map.entry("order_date", 4), java.util.Map.entry("total_amount", 5), java.util.Map.entry("version", 6));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("customer", java.util.List.of("customer", "customer")), java.util.Map.entry("status", java.util.List.of("status", "status")), java.util.Map.entry("order_number", java.util.List.of("orderNumber", "order_number")), java.util.Map.entry("order_date", java.util.List.of("orderDate", "order_date")), java.util.Map.entry("total_amount", java.util.List.of("totalAmount", "total_amount")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "orderItemList");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            CustomerOrder.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES));


    public static final String CUSTOMER_PROPERTY = "customer";
    public static final String STATUS_PROPERTY = "status";
    public static final String ORDER_NUMBER_PROPERTY = "orderNumber";
    public static final String ORDER_DATE_PROPERTY = "orderDate";
    public static final String TOTAL_AMOUNT_PROPERTY = "totalAmount";
    public static final String ORDER_ITEM_LIST_PROPERTY = "orderItemList";
    private Customer customer;

    private OrderStatus status;

    private String orderNumber;

    private LocalDate orderDate;

    private BigDecimal totalAmount;

    private SmartList<OrderItem> orderItemList;

    public Customer getCustomer(){
        return this.customer;
    }

    public OrderStatus getStatus(){
        return this.status;
    }

    public String getOrderNumber(){
        return this.orderNumber;
    }

    public LocalDate getOrderDate(){
        return this.orderDate;
    }

    public BigDecimal getTotalAmount(){
        return this.totalAmount;
    }

    public SmartList<OrderItem> getOrderItemList(){
        return this.orderItemList;
    }
    public CustomerOrder updateCustomer(Customer customer){
        if(Objects.equals(this.customer, customer)){
            return this;
        }
        handleUpdate(CUSTOMER_PROPERTY, getCustomer(), customer);
        this.customer = customer;
        return this;
    }

    protected CustomerOrder updateStatus(OrderStatus status){
        if(Objects.equals(this.status, status)){
            return this;
        }
        handleUpdate(STATUS_PROPERTY, getStatus(), status);
        this.status = status;
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

    public CustomerOrder updateOrderDate(LocalDate orderDate){
        if(Objects.equals(this.orderDate, orderDate)){
            return this;
        }
        handleUpdate(ORDER_DATE_PROPERTY, getOrderDate(), orderDate);
        this.orderDate = orderDate;
        return this;
    }

    public CustomerOrder updateTotalAmount(BigDecimal totalAmount){
        if(Objects.equals(this.totalAmount, totalAmount)){
            return this;
        }
        handleUpdate(TOTAL_AMOUNT_PROPERTY, getTotalAmount(), totalAmount);
        this.totalAmount = totalAmount;
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
    public boolean isStatusNew(){
        return Objects.equals(getStatus(), Constants.ORDER_STATUS_NEW);
    }

    public CustomerOrder updateStatusToNew(){
        return updateStatus(Constants.ORDER_STATUS_NEW);
    }
    public boolean isStatusPaid(){
        return Objects.equals(getStatus(), Constants.ORDER_STATUS_PAID);
    }

    public CustomerOrder updateStatusToPaid(){
        return updateStatus(Constants.ORDER_STATUS_PAID);
    }
    public boolean isStatusCancelled(){
        return Objects.equals(getStatus(), Constants.ORDER_STATUS_CANCELLED);
    }

    public CustomerOrder updateStatusToCancelled(){
        return updateStatus(Constants.ORDER_STATUS_CANCELLED);
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
            case "customer": this.customer = (Customer) value; break;

            case "status": this.status = (OrderStatus) value; break;

            case "orderNumber": this.orderNumber = (value == null ? null : ((String)value).trim()); break;

            case "orderDate": this.orderDate = (LocalDate) value; break;

            case "totalAmount": this.totalAmount = (BigDecimal) value; break;

            case "orderItemList": this.orderItemList = (SmartList<OrderItem>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "customer": return this.customer;
            case "status": return this.status;
            case "orderNumber": return this.orderNumber;
            case "orderDate": return this.orderDate;
            case "totalAmount": return this.totalAmount;
            case "orderItemList": return this.orderItemList;
            default: return super.__internalGet(property);
        }
    }

}