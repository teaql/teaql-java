
package com.example.requesttreeorderservice.orderitem;

import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
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
public class OrderItem extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "OrderItem";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "c42b4bc2252ab616b0b006ad2560900a40e2ca87d71ad860bab689f0b16b1795";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("customer_order", 1), java.util.Map.entry("product_name", 2), java.util.Map.entry("quantity", 3), java.util.Map.entry("unit_price", 4), java.util.Map.entry("version", 5));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("customer_order", java.util.List.of("customerOrder", "customer_order")), java.util.Map.entry("product_name", java.util.List.of("productName", "product_name")), java.util.Map.entry("quantity", java.util.List.of("quantity", "quantity")), java.util.Map.entry("unit_price", java.util.List.of("unitPrice", "unit_price")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            OrderItem.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES));


    public static final String CUSTOMER_ORDER_PROPERTY = "customerOrder";
    public static final String PRODUCT_NAME_PROPERTY = "productName";
    public static final String QUANTITY_PROPERTY = "quantity";
    public static final String UNIT_PRICE_PROPERTY = "unitPrice";
    private CustomerOrder customerOrder;

    private String productName;

    private Integer quantity;

    private BigDecimal unitPrice;


    public CustomerOrder getCustomerOrder(){
        return this.customerOrder;
    }

    public String getProductName(){
        return this.productName;
    }

    public Integer getQuantity(){
        return this.quantity;
    }

    public BigDecimal getUnitPrice(){
        return this.unitPrice;
    }

    public OrderItem updateCustomerOrder(CustomerOrder customerOrder){
        if(Objects.equals(this.customerOrder, customerOrder)){
            return this;
        }
        handleUpdate(CUSTOMER_ORDER_PROPERTY, getCustomerOrder(), customerOrder);
        this.customerOrder = customerOrder;
        return this;
    }

    public OrderItem updateProductName(String productName){
        productName = (productName == null ? null : productName.trim());
        if(Objects.equals(this.productName, productName)){
            return this;
        }
        handleUpdate(PRODUCT_NAME_PROPERTY, getProductName(), productName);
        this.productName = productName;
        return this;
    }

    public OrderItem updateQuantity(Integer quantity){
        if(Objects.equals(this.quantity, quantity)){
            return this;
        }
        handleUpdate(QUANTITY_PROPERTY, getQuantity(), quantity);
        this.quantity = quantity;
        return this;
    }

    public OrderItem updateUnitPrice(BigDecimal unitPrice){
        if(Objects.equals(this.unitPrice, unitPrice)){
            return this;
        }
        handleUpdate(UNIT_PRICE_PROPERTY, getUnitPrice(), unitPrice);
        this.unitPrice = unitPrice;
        return this;
    }


    public static OrderItem refer(Long id){
        OrderItem refer = new OrderItem();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public OrderItem comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<OrderItem> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "customerOrder": this.customerOrder = (CustomerOrder) value; break;

            case "productName": this.productName = (value == null ? null : ((String)value).trim()); break;

            case "quantity": this.quantity = (Integer) value; break;

            case "unitPrice": this.unitPrice = (BigDecimal) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "customerOrder": return this.customerOrder;
            case "productName": return this.productName;
            case "quantity": return this.quantity;
            case "unitPrice": return this.unitPrice;
            default: return super.__internalGet(property);
        }
    }

}