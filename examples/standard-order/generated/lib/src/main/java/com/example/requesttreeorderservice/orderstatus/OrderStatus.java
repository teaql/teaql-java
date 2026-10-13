
package com.example.requesttreeorderservice.orderstatus;

import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import com.example.requesttreeorderservice.platform.Platform;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
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

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "81304ab2e3fd61964bc163a850a2a321e1c0bfe204f85f36aedc4e1b5d8cebcd";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("platform", 0), java.util.Map.entry("id", 1), java.util.Map.entry("name", 2), java.util.Map.entry("code", 3), java.util.Map.entry("color", 4), java.util.Map.entry("display_order", 5), java.util.Map.entry("version", 6));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("platform", java.util.List.of("platform", "platform")), java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("code", java.util.List.of("code", "code")), java.util.Map.entry("color", java.util.List.of("color", "color")), java.util.Map.entry("display_order", java.util.List.of("displayOrder", "display_order")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "customerOrderList");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            OrderStatus.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES));


    public static final String PLATFORM_PROPERTY = "platform";
    public static final String NAME_PROPERTY = "name";
    public static final String CODE_PROPERTY = "code";
    public static final String COLOR_PROPERTY = "color";
    public static final String DISPLAY_ORDER_PROPERTY = "displayOrder";
    public static final String CUSTOMER_ORDER_LIST_PROPERTY = "customerOrderList";
    private Platform platform;

    private String name;

    private String code;

    private String color;

    private BigDecimal displayOrder;

    private SmartList<CustomerOrder> customerOrderList;

    public Platform getPlatform(){
        return this.platform;
    }

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

    public SmartList<CustomerOrder> getCustomerOrderList(){
        return this.customerOrderList;
    }
    public OrderStatus updatePlatform(Platform platform){
        if(Objects.equals(this.platform, platform)){
            return this;
        }
        handleUpdate(PLATFORM_PROPERTY, getPlatform(), platform);
        this.platform = platform;
        return this;
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

    public OrderStatus addCustomerOrder(CustomerOrder customerOrder){
        if (customerOrder == null){
            return this;
        }

        if(null == this.customerOrderList){
            this.customerOrderList = new SmartList<>();
        }

        this.customerOrderList.add(customerOrder);
        customerOrder.updateProperty(CustomerOrder.STATUS_PROPERTY, this);
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
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "platform": this.platform = (Platform) value; break;

            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "code": this.code = (value == null ? null : ((String)value).trim()); break;

            case "color": this.color = (value == null ? null : ((String)value).trim()); break;

            case "displayOrder": this.displayOrder = (BigDecimal) value; break;

            case "customerOrderList": this.customerOrderList = (SmartList<CustomerOrder>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "platform": return this.platform;
            case "name": return this.name;
            case "code": return this.code;
            case "color": return this.color;
            case "displayOrder": return this.displayOrder;
            case "customerOrderList": return this.customerOrderList;
            default: return super.__internalGet(property);
        }
    }

}