
package com.teaql.ordermanagementservice.orderstatus;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * [TEAQL AI WARNING]
 * TeaQL was explicitly designed to PREVENT AI hallucinations and random guessing.
 * DO NOT GUESS METHOD NAMES!
 * The methods listed below are the ONLY valid ways to interact with this entity.
 * If you encounter compilation errors (e.g., method not found), DO NOT guess another method name.
 * Read the method signatures in this file before proceeding.
 */
public class OrderStatus extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "OrderStatus";

    public static final String NAME_PROPERTY = "name";
    public static final String CODE_PROPERTY = "code";
    public static final String COLOR_PROPERTY = "color";
    public static final String DISPLAY_ORDER_PROPERTY = "displayOrder";
    public static final String COMMERCE_PLATFORM_PROPERTY = "commercePlatform";
    public static final String CUSTOMER_ORDER_LIST_PROPERTY = "customerOrderList";
    private String name;
    private String code;
    private String color;
    private BigDecimal displayOrder;
    private CommercePlatform commercePlatform;
    private SmartList<CustomerOrder> customerOrderList;

    public String getName(){
        return this.name;
    }
    public String getCode(){
        return this.code;
    }
    public String getColor(){
        return this.color;
    }
    public BigDecimal getDisplayOrder(){
        return this.displayOrder;
    }
    public CommercePlatform getCommercePlatform(){
        return this.commercePlatform;
    }
    public SmartList<CustomerOrder> getCustomerOrderList(){
        return this.customerOrderList;
    }
    public OrderStatus updateName(String name){
        name = (name == null ? null : name.trim());
        if(Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }
    public OrderStatus updateCode(String code){
        code = (code == null ? null : code.trim());
        if(Objects.equals(this.code, code)){
            return this;
        }
        handleUpdate(CODE_PROPERTY, getCode(), code);
        this.code = code;
        return this;
    }
    public OrderStatus updateColor(String color){
        color = (color == null ? null : color.trim());
        if(Objects.equals(this.color, color)){
            return this;
        }
        handleUpdate(COLOR_PROPERTY, getColor(), color);
        this.color = color;
        return this;
    }
    public OrderStatus updateDisplayOrder(BigDecimal displayOrder){
        if(Objects.equals(this.displayOrder, displayOrder)){
            return this;
        }
        handleUpdate(DISPLAY_ORDER_PROPERTY, getDisplayOrder(), displayOrder);
        this.displayOrder = displayOrder;
        return this;
    }
    public OrderStatus updateCommercePlatform(CommercePlatform commercePlatform){
        if(Objects.equals(this.commercePlatform, commercePlatform)){
            return this;
        }
        handleUpdate(COMMERCE_PLATFORM_PROPERTY, getCommercePlatform(), commercePlatform);
        this.commercePlatform = commercePlatform;
        return this;
    }
    public OrderStatus addCustomerOrder(CustomerOrder customerOrder){
        if (customerOrder == null){
            return this;
        }

        if(null == this.customerOrderList){
            this.customerOrderList = new SmartList<>();
        }

        this.customerOrderList.add(customerOrder);
        customerOrder.cacheRelation(CustomerOrder.STATUS_PROPERTY, this);
        return this;
    }

    public static OrderStatus refer(Long id){
        OrderStatus refer = new OrderStatus();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public OrderStatus comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<OrderStatus> auditAs(String action) {
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "code": this.code = (value == null ? null : ((String)value).trim()); break;

            case "color": this.color = (value == null ? null : ((String)value).trim()); break;

            case "displayOrder": this.displayOrder = (BigDecimal) value; break;

            case "commercePlatform": this.commercePlatform = (CommercePlatform) value; break;

            case "customerOrderList": this.customerOrderList = (SmartList<CustomerOrder>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "code": return this.code;
            case "color": return this.color;
            case "displayOrder": return this.displayOrder;
            case "commercePlatform": return this.commercePlatform;
            case "customerOrderList": return this.customerOrderList;
            default: return super.__internalGet(property);
        }
    }

}