
package com.teaql.ordermanagementservice.customerorder;

import com.teaql.ordermanagementservice.Constants;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderstatus.OrderStatus;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
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

    public static final String ORDER_NUMBER_PROPERTY = "orderNumber";
    public static final String ORDER_DATE_PROPERTY = "orderDate";
    public static final String TOTAL_AMOUNT_PROPERTY = "totalAmount";
    public static final String STATUS_PROPERTY = "status";
    public static final String CUSTOMER_PROPERTY = "customer";
    public static final String COMMERCE_PLATFORM_PROPERTY = "commercePlatform";
    public static final String CREATE_TIME_PROPERTY = "createTime";
    public static final String UPDATE_TIME_PROPERTY = "updateTime";
    public static final String ORDER_LINE_LIST_PROPERTY = "orderLineList";
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
        return this.orderLineList;
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
    protected CustomerOrder updateStatus(OrderStatus status){
        if(Objects.equals(this.status, status)){
            return this;
        }
        handleUpdate(STATUS_PROPERTY, getStatus(), status);
        this.status = status;
        return this;
    }
    public CustomerOrder updateCustomer(Customer customer){
        if(Objects.equals(this.customer, customer)){
            return this;
        }
        handleUpdate(CUSTOMER_PROPERTY, getCustomer(), customer);
        this.customer = customer;
        return this;
    }
    public CustomerOrder updateCommercePlatform(CommercePlatform commercePlatform){
        if(Objects.equals(this.commercePlatform, commercePlatform)){
            return this;
        }
        handleUpdate(COMMERCE_PLATFORM_PROPERTY, getCommercePlatform(), commercePlatform);
        this.commercePlatform = commercePlatform;
        return this;
    }
    public CustomerOrder updateCreateTime(LocalDateTime createTime){
        if(Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }
    public CustomerOrder updateUpdateTime(LocalDateTime updateTime){
        if(Objects.equals(this.updateTime, updateTime)){
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
            this.orderLineList = new SmartList<>();
        }

        this.orderLineList.add(orderLine);
        orderLine.cacheRelation(OrderLine.CUSTOMER_ORDER_PROPERTY, this);
        return this;
    }
    public boolean isStatusPending(){
        return Objects.equals(getStatus(), Constants.ORDER_STATUS_PENDING);
    }

    public CustomerOrder updateStatusToPending(){
        return updateStatus(Constants.ORDER_STATUS_PENDING);
    }
    public boolean isStatusProcessing(){
        return Objects.equals(getStatus(), Constants.ORDER_STATUS_PROCESSING);
    }

    public CustomerOrder updateStatusToProcessing(){
        return updateStatus(Constants.ORDER_STATUS_PROCESSING);
    }
    public boolean isStatusShipped(){
        return Objects.equals(getStatus(), Constants.ORDER_STATUS_SHIPPED);
    }

    public CustomerOrder updateStatusToShipped(){
        return updateStatus(Constants.ORDER_STATUS_SHIPPED);
    }
    public boolean isStatusCompleted(){
        return Objects.equals(getStatus(), Constants.ORDER_STATUS_COMPLETED);
    }

    public CustomerOrder updateStatusToCompleted(){
        return updateStatus(Constants.ORDER_STATUS_COMPLETED);
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
        switch (property) {
            case "orderNumber": this.orderNumber = (value == null ? null : ((String)value).trim()); break;

            case "orderDate": this.orderDate = (LocalDate) value; break;

            case "totalAmount": this.totalAmount = (BigDecimal) value; break;

            case "status": this.status = (OrderStatus) value; break;

            case "customer": this.customer = (Customer) value; break;

            case "commercePlatform": this.commercePlatform = (CommercePlatform) value; break;

            case "createTime": this.createTime = (LocalDateTime) value; break;

            case "updateTime": this.updateTime = (LocalDateTime) value; break;

            case "orderLineList": this.orderLineList = (SmartList<OrderLine>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "orderNumber": return this.orderNumber;
            case "orderDate": return this.orderDate;
            case "totalAmount": return this.totalAmount;
            case "status": return this.status;
            case "customer": return this.customer;
            case "commercePlatform": return this.commercePlatform;
            case "createTime": return this.createTime;
            case "updateTime": return this.updateTime;
            case "orderLineList": return this.orderLineList;
            default: return super.__internalGet(property);
        }
    }

}