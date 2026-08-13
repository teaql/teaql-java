
package com.teaql.ordermanagementservice.customer;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
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
public class Customer extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "Customer";

    public static final String NAME_PROPERTY = "name";
    public static final String EMAIL_PROPERTY = "email";
    public static final String COMMERCE_PLATFORM_PROPERTY = "commercePlatform";
    public static final String CREATE_TIME_PROPERTY = "createTime";
    public static final String UPDATE_TIME_PROPERTY = "updateTime";
    public static final String CUSTOMER_ORDER_LIST_PROPERTY = "customerOrderList";
    private String name;
    private String email;
    private CommercePlatform commercePlatform;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private SmartList<CustomerOrder> customerOrderList;

    public String getName(){
        return this.name;
    }
    public String getEmail(){
        return this.email;
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
    public SmartList<CustomerOrder> getCustomerOrderList(){
        return this.customerOrderList;
    }
    public Customer updateName(String name){
        name = (name == null ? null : name.trim());
        if(Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }
    public Customer updateEmail(String email){
        email = (email == null ? null : email.trim());
        if(Objects.equals(this.email, email)){
            return this;
        }
        handleUpdate(EMAIL_PROPERTY, getEmail(), email);
        this.email = email;
        return this;
    }
    public Customer updateCommercePlatform(CommercePlatform commercePlatform){
        if(Objects.equals(this.commercePlatform, commercePlatform)){
            return this;
        }
        handleUpdate(COMMERCE_PLATFORM_PROPERTY, getCommercePlatform(), commercePlatform);
        this.commercePlatform = commercePlatform;
        return this;
    }
    public Customer updateCreateTime(LocalDateTime createTime){
        if(Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }
    public Customer updateUpdateTime(LocalDateTime updateTime){
        if(Objects.equals(this.updateTime, updateTime)){
            return this;
        }
        handleUpdate(UPDATE_TIME_PROPERTY, getUpdateTime(), updateTime);
        this.updateTime = updateTime;
        return this;
    }
    public Customer addCustomerOrder(CustomerOrder customerOrder){
        if (customerOrder == null){
            return this;
        }

        if(null == this.customerOrderList){
            this.customerOrderList = new SmartList<>();
        }

        this.customerOrderList.add(customerOrder);
        customerOrder.cacheRelation(CustomerOrder.CUSTOMER_PROPERTY, this);
        return this;
    }

    public static Customer refer(Long id){
        Customer refer = new Customer();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public Customer comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<Customer> auditAs(String action) {
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "email": this.email = (value == null ? null : ((String)value).trim()); break;

            case "commercePlatform": this.commercePlatform = (CommercePlatform) value; break;

            case "createTime": this.createTime = (LocalDateTime) value; break;

            case "updateTime": this.updateTime = (LocalDateTime) value; break;

            case "customerOrderList": this.customerOrderList = (SmartList<CustomerOrder>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "email": return this.email;
            case "commercePlatform": return this.commercePlatform;
            case "createTime": return this.createTime;
            case "updateTime": return this.updateTime;
            case "customerOrderList": return this.customerOrderList;
            default: return super.__internalGet(property);
        }
    }

}