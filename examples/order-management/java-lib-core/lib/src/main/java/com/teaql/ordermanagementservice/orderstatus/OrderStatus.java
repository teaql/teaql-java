
package com.teaql.ordermanagementservice.orderstatus;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
import io.teaql.core.checker.JsonFieldNamingProfile;
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
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "f26f586dd82cdacffeee3bb7fe1ac1ef7385535d6ef257cd764ee435607e21c9";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("name", 1), java.util.Map.entry("code", 2), java.util.Map.entry("color", 3), java.util.Map.entry("display_order", 4), java.util.Map.entry("commerce_platform", 5), java.util.Map.entry("version", 6));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("code", java.util.List.of("code", "code")), java.util.Map.entry("color", java.util.List.of("color", "color")), java.util.Map.entry("display_order", java.util.List.of("displayOrder", "display_order")), java.util.Map.entry("commerce_platform", java.util.List.of("commercePlatform", "commerce_platform")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "customer_order_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            OrderStatus.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String NAME_PROPERTY = "name";
    public static final String CODE_PROPERTY = "code";
    public static final String COLOR_PROPERTY = "color";
    public static final String DISPLAY_ORDER_PROPERTY = "display_order";
    public static final String COMMERCE_PLATFORM_PROPERTY = "commerce_platform";
    public static final String CUSTOMER_ORDER_LIST_PROPERTY = "customer_order_list";
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
        return this.customerOrderList == null ? SmartList.unloaded() : this.customerOrderList;
    }
    public OrderStatus updateName(String name){
        name = (name == null ? null : name.trim());
        if(isPropertyLoaded(NAME_PROPERTY) && Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public OrderStatus updateCode(String code){
        code = (code == null ? null : code.trim());
        if(isPropertyLoaded(CODE_PROPERTY) && Objects.equals(this.code, code)){
            return this;
        }
        handleUpdate(CODE_PROPERTY, getCode(), code);
        this.code = code;
        return this;
    }

    public OrderStatus updateColor(String color){
        color = (color == null ? null : color.trim());
        if(isPropertyLoaded(COLOR_PROPERTY) && Objects.equals(this.color, color)){
            return this;
        }
        handleUpdate(COLOR_PROPERTY, getColor(), color);
        this.color = color;
        return this;
    }

    public OrderStatus updateDisplayOrder(BigDecimal displayOrder){
        if(isPropertyLoaded(DISPLAY_ORDER_PROPERTY) && Objects.equals(this.displayOrder, displayOrder)){
            return this;
        }
        handleUpdate(DISPLAY_ORDER_PROPERTY, getDisplayOrder(), displayOrder);
        this.displayOrder = displayOrder;
        return this;
    }

    public OrderStatus updateCommercePlatform(CommercePlatform commercePlatform){
        if(isPropertyLoaded(COMMERCE_PLATFORM_PROPERTY) && Objects.equals(this.commercePlatform, commercePlatform)){
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
            SmartList<CustomerOrder> teaqlUnloadedItems = SmartList.unloaded();
            this.customerOrderList = teaqlUnloadedItems.map(value -> value);
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
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "code": this.code = (value == null ? null : ((String)value).trim()); break;

            case "color": this.color = (value == null ? null : ((String)value).trim()); break;

            case "display_order": this.displayOrder = (BigDecimal) value; break;

            case "commerce_platform": this.commercePlatform = (CommercePlatform) value; break;

            case "customer_order_list": this.customerOrderList = (SmartList<CustomerOrder>) value; break;
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
            case "display_order": return this.displayOrder;
            case "commerce_platform": return this.commercePlatform;
            case "customer_order_list": return this.customerOrderList;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "name": return this.name;
                    case "code": return this.code;
                    case "color": return this.color;
                    case "displayOrder": return this.displayOrder;
                    case "commercePlatform": return this.commercePlatform;
                    default: return super.__internalGet(property);
                }
        }
    }

}